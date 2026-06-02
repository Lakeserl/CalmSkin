package com.lakeserl.api_gateway.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final String internalSecret;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, StringRedisTemplate redisTemplate,
            @Value("${app.internal-secret}") String internalSecret) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.internalSecret = internalSecret;
    }

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/resend-verification",
            "/api/v1/auth/oauth2/google",
            // Payment provider IPN/webhook callbacks arrive with no Authorization header.
            // Invariant §9: gateway blocks /internal/** — webhook paths are /api/v1/payments/webhook/**.
            "/api/v1/payments/webhook/",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator/health"
    );

    // PROJECT_NOTES §6.1: catalogue browsing is public, but only for GET.
    // Writes on these resources live under /api/v1/admin/** and stay protected.
    private static final Set<String> PUBLIC_GET_PREFIXES = Set.of(
            "/api/v1/products",
            "/api/v1/categories",
            "/api/v1/brands",
            "/api/v1/ingredients",
            "/api/v1/reviews"
    );

    // Headers that must only be written by this gateway — strip from all external requests.
    private static final Set<String> INTERNAL_HEADERS = Set.of(
            "X-User-Id", "X-User-Role", "X-User-Email", "X-User-Jti",
            "X-Gateway-Secret", "X-Internal-Secret");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Strip client-supplied identity/secret headers, then re-add X-Gateway-Secret so
        // downstream services can verify the request came through this gateway.
        ServerHttpRequest stripped = exchange.getRequest().mutate()
                .headers(h -> INTERNAL_HEADERS.forEach(h::remove))
                .header("X-Gateway-Secret", internalSecret)
                .build();
        exchange = exchange.mutate().request(stripped).build();

        String path = exchange.getRequest().getPath().value();

        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        if (isPublicPath(path) || isPublicGetPath(exchange)) {
            return chain.filter(exchange);
        }

        if (path.startsWith("/internal/")) {
            return reject(exchange, HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "MISSING_TOKEN");
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtUtil.validateAndExtractClaims(token);
            String jti = claims.getId();

            if (jti != null) {
                Boolean isBlacklisted = redisTemplate.hasKey("blacklist_token:" + jti);
                if (Boolean.TRUE.equals(isBlacklisted)) {
                    return reject(exchange, HttpStatus.UNAUTHORIZED, "TOKEN_REVOKED");
                }
            }

            String userId = claims.get("userId", String.class);
            String email = claims.get("email", String.class);
            String role = extractRole(claims);

            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
            if (userId != null) {
                requestBuilder.header("X-User-Id", userId);
            } else if (claims.getSubject() != null) {
                requestBuilder.header("X-User-Id", claims.getSubject());
            }
            if (role != null) {
                requestBuilder.header("X-User-Role", role);
            }
            if (email != null) {
                requestBuilder.header("X-User-Email", email);
            }
            if (jti != null) {
                requestBuilder.header("X-User-Jti", jti);
            }

            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());

        } catch (ExpiredJwtException e) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED");
        } catch (JwtException e) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "TOKEN_INVALID");
        }
    }

    private boolean isPublicPath(String path) {
        for (String prefix : PUBLIC_PATHS) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPublicGetPath(ServerWebExchange exchange) {
        if (!HttpMethod.GET.equals(exchange.getRequest().getMethod())) {
            return false;
        }
        String path = exchange.getRequest().getPath().value();
        for (String prefix : PUBLIC_GET_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String extractRole(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list && !list.isEmpty()) {
            return String.valueOf(list.get(0));
        }
        Object role = claims.get("role");
        return role != null ? role.toString() : null;
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String code) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"success":false,"code":"%s","message":"%s","timestamp":"%s"}
                """.formatted(code, status.getReasonPhrase(), Instant.now());
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}

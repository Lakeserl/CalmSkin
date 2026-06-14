package com.lakeserl.user_service.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class InternalApiSecurityFilter implements Filter {

    @Value("${app.internal.secret}")
    private String internalSecret;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;

        if (request.getRequestURI().startsWith("/internal/")) {
            String secret = request.getHeader("X-Internal-Secret");
            if (secret == null || !java.security.MessageDigest.isEqual(
                    secret.getBytes(StandardCharsets.UTF_8),
                    internalSecret.getBytes(StandardCharsets.UTF_8))) {
                HttpServletResponse response = (HttpServletResponse) res;
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                String body = """
                        {"success":false,"code":"FORBIDDEN","message":"Internal access denied","timestamp":"%s"}
                        """.formatted(Instant.now());
                response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
                return;
            }
        }

        chain.doFilter(req, res);
    }
}

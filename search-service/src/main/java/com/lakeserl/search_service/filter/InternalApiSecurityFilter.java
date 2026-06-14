package com.lakeserl.search_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class InternalApiSecurityFilter extends OncePerRequestFilter {

    @Value("${app.internal.secret}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/internal/")) {
            String secret = request.getHeader("X-Internal-Secret");
            if (secret == null || !java.security.MessageDigest.isEqual(
                    internalSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                log.warn("Internal API rejected — missing or wrong secret from {}", request.getRemoteAddr());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}

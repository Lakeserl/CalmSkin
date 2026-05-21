package com.lakeserl.notification_service.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Builds the SecurityContext from the X-User-Id / X-User-Role headers that
 * api-gateway injects after validating the JWT. Downstream services never
 * parse the JWT themselves.
 */
public class RoleHeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String roleHeader = request.getHeader("X-User-Role");
        String userId = request.getHeader("X-User-Id");

        if (userId != null && roleHeader != null && !roleHeader.isBlank()) {
            List<SimpleGrantedAuthority> authorities;
            if (roleHeader.contains(",")) {
                authorities = Arrays.stream(roleHeader.split(","))
                        .map(role -> new SimpleGrantedAuthority(toAuthority(role)))
                        .collect(Collectors.toList());
            } else {
                authorities = List.of(new SimpleGrantedAuthority(toAuthority(roleHeader)));
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String toAuthority(String role) {
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}

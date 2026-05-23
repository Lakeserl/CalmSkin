package com.lakeserl.product_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RoleHeaderAuthenticationFilter extends OncePerRequestFilter {

    private final String expectedGatewaySecret;

    public RoleHeaderAuthenticationFilter(String expectedGatewaySecret) {
        this.expectedGatewaySecret = expectedGatewaySecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
            
        String gatewaySecret = request.getHeader("X-Gateway-Secret");

        if (expectedGatewaySecret.equals(gatewaySecret)) {
            String roleHeader = request.getHeader("X-User-Role");
            String userId = request.getHeader("X-User-Id");

            if (userId != null && roleHeader != null && !roleHeader.isEmpty()) {
                // Gateway sends a single role value (e.g. "ROLE_ADMIN" or "ADMIN")
                // Handle both formats: with or without "ROLE_" prefix
                List<SimpleGrantedAuthority> authorities;
                if (roleHeader.contains(",")) {
                    // Multiple roles (comma-separated)
                    authorities = Arrays.stream(roleHeader.split(","))
                            .map(role -> {
                                String r = role.trim().toUpperCase();
                                return new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r);
                            })
                            .collect(Collectors.toList());
                } else {
                    // Single role
                    String r = roleHeader.trim().toUpperCase();
                    String authority = r.startsWith("ROLE_") ? r : "ROLE_" + r;
                    authorities = List.of(new SimpleGrantedAuthority(authority));
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, authorities);
                        
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}

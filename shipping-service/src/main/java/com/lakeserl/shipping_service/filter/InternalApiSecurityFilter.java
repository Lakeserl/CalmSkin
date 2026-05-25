package com.lakeserl.shipping_service.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakeserl.shipping_service.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class InternalApiSecurityFilter extends OncePerRequestFilter {

    @Value("${app.internal.secret:${internal.secret:}}")
    private String expectedSecret;

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/internal/")) {
            String secretHeader = request.getHeader("X-Internal-Secret");
            if (secretHeader == null || !secretHeader.equals(expectedSecret)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ApiResponse<Void> error = ApiResponse.error(
                        "FORBIDDEN_INTERNAL_ACCESS", "Missing or invalid internal secret");
                objectMapper.writeValue(response.getWriter(), error);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}

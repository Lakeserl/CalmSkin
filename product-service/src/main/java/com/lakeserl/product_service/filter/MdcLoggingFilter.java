package com.lakeserl.product_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Get Request ID from gateway header or generate a new one
            String requestId = request.getHeader("X-Request-Id");
            if (requestId == null) {
                requestId = UUID.randomUUID().toString().substring(0, 8);
            }
            
            MDC.put("requestId", requestId);
            MDC.put("method", request.getMethod());
            MDC.put("uri", request.getRequestURI());
            MDC.put("ip", request.getRemoteAddr());

            long start = System.currentTimeMillis();
            filterChain.doFilter(request, response);
            long duration = System.currentTimeMillis() - start;

            MDC.put("duration", duration + "ms");
            MDC.put("status", String.valueOf(response.getStatus()));
        } finally {
            MDC.clear();
        }
    }
}

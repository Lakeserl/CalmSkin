package com.lakeserl.ai_recommendation_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // /similar and /trending are public; /for-me and /frequently-bought-with require auth
                // Auth is validated upstream by api-gateway JWT filter; this service trusts X-User-Id header
                .requestMatchers(
                    "/api/v1/recommendations/similar/**",
                    "/api/v1/recommendations/trending",
                    "/api/v1/recommendations/frequently-bought-with/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/actuator/prometheus",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .anyRequest().permitAll()  // JWT validated by gateway; all requests arriving here are pre-authed
            );
        return http.build();
    }
}

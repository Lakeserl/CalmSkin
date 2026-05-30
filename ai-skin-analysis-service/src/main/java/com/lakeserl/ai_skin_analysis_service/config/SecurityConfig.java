package com.lakeserl.ai_skin_analysis_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.internal.secret}")
    private String internalSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/swagger-ui/**",
                    "/docs",
                    "/v3/api-docs/**",
                    "/actuator/**",
                    "/internal/**"
                ).permitAll()
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "STAFF")
                .anyRequest().authenticated()
            );

        http.addFilterBefore(
            new RoleHeaderAuthenticationFilter(internalSecret),
            UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}

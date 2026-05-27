package com.lakeserl.shipping_service.config;

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

    @Value("${internal.secret}")
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
                    "/internal/**",
                    // Webhook auth is handled inside the controller via shared
                    // secret header; bypass Spring Security's role check here.
                    "/webhooks/**"
                ).permitAll()
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "STAFF")
                // Customer-facing endpoints: authenticated users can only see their own data
                // (ownership enforcement is done inside CustomerShipmentController via X-User-Id)
                .requestMatchers("/api/v1/shipments/**").authenticated()
                .anyRequest().authenticated()
            );

        http.addFilterBefore(new RoleHeaderAuthenticationFilter(internalSecret),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

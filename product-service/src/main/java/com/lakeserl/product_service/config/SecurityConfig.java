package com.lakeserl.product_service.config;

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

    @Value("${app.internal.secret:${app.internal-secret:${internal.secret:}}}")
    private String internalSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/v1/products/**",
                    "/api/v1/categories/**",
                    "/api/v1/brands/**",
                    "/api/v1/ingredients/**",
                    "/swagger-ui/**",
                    "/docs",
                    "/v3/api-docs/**",
                    "/actuator/**",
                    "/internal/**" // internal is validated via a custom filter or header, not JWT
                ).permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            );

        // Note: For a microservice that relies on Gateway for authentication validation
        // and JWT parsing, we typically create a custom filter to extract the user roles
        // from headers (e.g. X-User-Role) sent by the Gateway.
        // For simplicity and since we don't have JwtAuthenticationFilter here,
        // we assume the gateway already handles auth, but we still need Spring Security 
        // to recognize the roles. If this service needs to validate JWT directly, 
        // we would copy JwtAuthenticationFilter from user-service.
        // Currently, we will just use a simple filter to set the Authentication object based on headers.

        http.addFilterBefore(new RoleHeaderAuthenticationFilter(internalSecret), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

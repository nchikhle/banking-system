package com.logiqpool.auditservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.web.servlet.function.RequestPredicates.headers;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 3. Disable CSRF for H2 actions --globaly
                .csrf(csrf -> csrf.disable())
        // 1. You must use /** to allow all internal H2 Console pages
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/logs", "/api/v1/accounts").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 2. Prevent Spring Security from blocking frame framesets
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

                //.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));

        return http.build();
    }
}

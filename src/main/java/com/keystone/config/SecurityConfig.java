package com.keystone.config;

import com.keystone.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                // JWT based REST API ke liye CSRF disabled
                .csrf(csrf -> csrf.disable())

                // Session use nahi hogi
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // =========================
                        // AUTHENTICATION
                        // Login public hai
                        // =========================
                        .requestMatchers("/api/auth/**")
                        .permitAll()

                        // =========================
                        // CUSTOMERS
                        // Customer APIs ke liye
                        // JWT required
                        // Actual role check @PreAuthorize karega
                        // =========================
                        .requestMatchers("/api/customers/**")
                        .authenticated()

                        // =========================
                        // SITES
                        // Site APIs ke liye JWT required
                        // =========================
                        .requestMatchers("/api/sites/**")
                        .authenticated()

                        // =========================
                        // USERS
                        // JWT required
                        // =========================
                        .requestMatchers("/api/users/**")
                        .authenticated()

                        // =========================
                        // WORK ORDERS
                        // Abhi Day 6 mein public
                        // Day 7-8 mein properly secure karenge
                        // =========================
                        .requestMatchers("/api/work-orders/**")
                        .permitAll()

                        // =========================
                        // BAaki sab
                        // =========================
                        .anyRequest()
                        .authenticated()
                )

                // JWT filter ko Spring Security chain mein add karo
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
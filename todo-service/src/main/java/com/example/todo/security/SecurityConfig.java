package com.example.todo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtFilter filter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors->cors.configurationSource(request -> {
                    var config=new CorsConfiguration();
                    config.setAllowedOrigins(List.of("http://localhost:5173"));
                    config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                    config.setAllowCredentials(true);
                    config.setAllowedHeaders(java.util.List.of("*"));
                    return config;
                }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/todos/**").authenticated()
                        .requestMatchers("/api/todos").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
//                .formLogin(form -> form.disable())   // Disable default login page
//                .httpBasic(basic -> basic.disable()); // Disable basic auth

        return http.build();
    }
}

package com.example.auth.security;

import com.example.auth.security.oauth.CustomOAuth2UserService;
import com.example.auth.security.oauth.OAuth2LoginSuccessHandler;
import com.example.auth.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtProvider jwtProvider, CustomUserDetailsService userDetailsService) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            CustomOAuth2UserService oAuth2UserService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler
    ) throws Exception {

        var jwtFilter = new JwtAuthenticationFilter(jwtProvider, userDetailsService);

        http
                .cors(cors -> cors.configurationSource(req -> {
                    var c = new org.springframework.web.cors.CorsConfiguration();
                    c.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:8081"));
                    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    c.setAllowedHeaders(List.of("*"));
                    c.setAllowCredentials(true);
                    return c;
                }))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**").permitAll()
                        .anyRequest().authenticated()
                );

        http.oauth2Login(oauth -> oauth
                .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                .successHandler(oAuth2LoginSuccessHandler)
        );

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}

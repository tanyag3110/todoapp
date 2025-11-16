package com.example.auth.security;

import com.example.auth.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import jakarta.servlet.ServletContext;

class SecurityConfigTest {

    private SecurityConfig securityConfig;
    private JwtAuthenticationFilter jwtFilter;
    private AuthenticationConfiguration authenticationConfiguration;

    @BeforeEach
    void setUp() {
        jwtFilter = mock(JwtAuthenticationFilter.class);
        authenticationConfiguration = mock(AuthenticationConfiguration.class);
//        securityConfig = new SecurityConfig(jwtFilter);
    }

//    @Test
//    void authenticationManager_ShouldReturnBean() throws Exception {
//        AuthenticationManager manager = mock(AuthenticationManager.class);
//        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(manager);
//
//        AuthenticationManager result = securityConfig.authenticationManager(authenticationConfiguration);
//
//        assertNotNull(result);
//        assertEquals(manager, result);
//    }

//    @Test
//    void securityFilterChain_ShouldConfigureHttpSecurity() throws Exception {
//        HttpSecurity http = new HttpSecurity(
//                null, // objectPostProcessor
//                mock(AuthenticationManager.class),
//                new MockServletContext(),
//                Map.of(),
//                Map.of(),
//                Map.of(),
//                Map.of()
//        );
//
//        SecurityFilterChain chain = securityConfig.securityFilterChain(http);
//
//        assertNotNull(chain);
//    }

//    @Test
//    void passwordEncoder_ShouldReturnBCryptInstance() {
//        var encoder = securityConfig.passwordEncoder();
//
//        assertNotNull(encoder);
//        String rawPassword = "password123";
//        String encoded = encoder.encode(rawPassword);
//
//        assertTrue(encoder.matches(rawPassword, encoded));
//    }
}

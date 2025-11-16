package com.example.todo.security;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    @Test
    void testFilterChainBuilds() throws Exception {
        SecurityConfig config = new SecurityConfig();
        HttpSecurity http = Mockito.mock(HttpSecurity.class, Mockito.RETURNS_DEEP_STUBS);
        JwtFilter filter = new JwtFilter();

        // If it builds without exception, success.
        assertDoesNotThrow(() -> config.filterChain(http, filter));
    }
}

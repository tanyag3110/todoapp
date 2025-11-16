package com.example.auth.security;

import com.example.auth.service.CustomUserDetailsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    @Mock JwtProvider jwtProvider;
    @Mock CustomUserDetailsService userDetailsService;
    @Mock FilterChain filterChain;

    JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); filter = new JwtAuthenticationFilter(jwtProvider, userDetailsService); }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void validHeader_setsAuthentication() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer sometoken");
        MockHttpServletResponse res = new MockHttpServletResponse();

        when(jwtProvider.getUsernameFromToken("sometoken")).thenReturn("alice");
        var userDetails = org.springframework.security.core.userdetails.User.withUsername("alice").password("p").roles("USER").build();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);

        filter.doFilterInternal(req, res, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("alice");
        verify(filterChain).doFilter(req, res);
    }

    @Test
    void invalidToken_doesNotSetAuthentication() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer badtoken");
        MockHttpServletResponse res = new MockHttpServletResponse();

        when(jwtProvider.getUsernameFromToken("badtoken")).thenThrow(new RuntimeException("invalid"));

        filter.doFilterInternal(req, res, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verify(filterChain).doFilter(req, res);
    }

    @Test
    void noHeader_proceedsWithoutAuth() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilterInternal(req, res, filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(req, res);
    }
}

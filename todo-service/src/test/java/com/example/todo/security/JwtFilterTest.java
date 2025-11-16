package com.example.todo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.mockito.Mockito.*;

class JwtFilterTest {

    @InjectMocks
    private JwtFilter filter;

    @Mock
    private FilterChain chain;

    @Mock
    private HttpServletRequest req;

    @Mock
    private HttpServletResponse res;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        filter = new JwtFilter();
        filter.secret = "mysecretkeymysecretkeymysecretkey123"; // >= 32 bytes
        SecurityContextHolder.clearContext();
    }

    private String generateToken(String username) {
        Key key = Keys.hmacShaKeyFor(filter.secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 10000))
                .signWith(key)
                .compact();
    }

    @Test
    void testValidToken() throws Exception {
        String token = generateToken("userA");
        when(req.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        verify(req).setAttribute("username", "userA");
        Assertions.assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testInvalidToken() throws Exception {
        when(req.getHeader("Authorization")).thenReturn("Bearer invalidtoken");

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
        // should NOT set username
        verify(req, never()).setAttribute(eq("username"), any());
    }

    @Test
    void testNoHeader() throws Exception {
        when(req.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(req, res, chain);

        verify(chain).doFilter(req, res);
    }
}

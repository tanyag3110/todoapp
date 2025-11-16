package com.example.auth.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JwtProviderTest {

    // secret must be at least 32 bytes for HS256 key
    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void generateAndValidate_tokensWork() {
        JwtProvider p = new JwtProvider(SECRET, 60L, 600L);
        String token = p.generateAccessToken("alice");
        assertThat(token).isNotBlank();
        String username = p.getUsernameFromToken(token);
        assertThat(username).isEqualTo("alice");

        String refresh = p.generateRefreshToken("bob");
        assertThat(refresh).isNotBlank();
        assertThat(p.getUsernameFromToken(refresh)).isEqualTo("bob");

        // validate token returns Jws<Claims> without throwing
        var jws = p.validateToken(token);
        assertThat(jws.getBody().getSubject()).isEqualTo("alice");
    }
}

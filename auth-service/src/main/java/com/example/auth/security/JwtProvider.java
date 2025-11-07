package com.example.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtProvider {

    private final Key key;
    private final long accessValiditySeconds;
    private final long refreshValiditySeconds;

    public JwtProvider(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.accessTokenValiditySeconds}") long accessValiditySeconds,
                       @Value("${app.jwt.refreshTokenValiditySeconds}") long refreshValiditySeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessValiditySeconds = accessValiditySeconds;
        this.refreshValiditySeconds = refreshValiditySeconds;
    }

    public String generateAccessToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessValiditySeconds * 1000);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshValiditySeconds * 1000);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("typ","refresh")
                .signWith(key)
                .compact();
    }

    public Jws<Claims> validateToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

    public String getUsernameFromToken(String token) {
        return validateToken(token).getBody().getSubject();
    }

    public long getAccessValiditySeconds() {
        return accessValiditySeconds;
    }

    public long getRefreshValiditySeconds() {
        return refreshValiditySeconds;
    }
}

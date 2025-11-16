package com.example.todo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Value("${app.jwt.secret}")
    String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, java.io.IOException {

        String header = req.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String username = claims.getSubject();
                System.out.println("✅ Authenticated user from JWT: " + username);

                // ✅ Set attribute for controller (optional)
                req.setAttribute("username", username);

                // ✅ Set authentication in SecurityContext
                var principal = new User(username, "", Collections.emptyList());
                var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                System.out.println("❌ JWT validation failed: " + e.getMessage());
            }
        }

        chain.doFilter(req, res);
    }
}

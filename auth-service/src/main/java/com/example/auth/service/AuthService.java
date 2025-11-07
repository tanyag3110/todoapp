package com.example.auth.service;

import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.LoginRequest;
import com.example.auth.entity.RefreshToken;
import com.example.auth.entity.User;
import com.example.auth.repository.RefreshTokenRepository;
import com.example.auth.repository.UserLogRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.JwtProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshRepo;
    private final JwtProvider jwt;
    private final BCryptPasswordEncoder encoder;
    private final UserService userService;
    private final UserLogRepository logRepo;

    public AuthService(UserRepository userRepo,
                       RefreshTokenRepository refreshRepo,
                       JwtProvider jwt,
                       BCryptPasswordEncoder encoder,
                       UserService userService,
                       UserLogRepository logRepo) {
        this.userRepo = userRepo;
        this.refreshRepo = refreshRepo;
        this.jwt = jwt;
        this.encoder = encoder;
        this.userService = userService;
        this.logRepo = logRepo;
    }

    public AuthResponse login(LoginRequest req, String ip) {
        var uOpt = userRepo.findByUsername(req.getUsername());
        if (uOpt.isEmpty()) throw new IllegalArgumentException("Invalid credentials");
        User u = uOpt.get();

        if (!u.isEnabled()) throw new IllegalStateException("Account not confirmed");
        if (u.isLocked()) throw new IllegalStateException("Account locked");

        if (!encoder.matches(req.getPassword(), u.getPasswordHash())) {
            userService.registerFailedLogin(u, ip);
            throw new IllegalArgumentException("Invalid credentials");
        }

        userService.resetFailedAttempts(u);
        String access = jwt.generateAccessToken(u.getUsername());
        String refresh = jwt.generateRefreshToken(u.getUsername());

        RefreshToken r = RefreshToken.builder()
                .token(refresh)
                .user(u)
                .expiryDate(Instant.now().plusSeconds(jwt.getRefreshValiditySeconds()))
                .revoked(false)
                .build();
        refreshRepo.save(r);

        logRepo.save(userService.createLog(u, "TOKEN_ISSUED", ip, "Issued tokens"));
        return new AuthResponse(access, refresh, jwt.getAccessValiditySeconds());
    }

    public AuthResponse refresh(String refreshToken, String ip) {
        var rOpt = refreshRepo.findByToken(refreshToken);
        if (rOpt.isEmpty()) throw new IllegalArgumentException("Invalid refresh token");
        RefreshToken r = rOpt.get();
        if (r.isRevoked() || r.getExpiryDate().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token invalid or expired");
        }
        String username = jwt.getUsernameFromToken(refreshToken);
        String access = jwt.generateAccessToken(username);
        // rotate refresh token:
        String newRefresh = jwt.generateRefreshToken(username);
        r.setToken(newRefresh);
        r.setExpiryDate(Instant.now().plusSeconds(jwt.getRefreshValiditySeconds()));
        refreshRepo.save(r);

        var u = r.getUser();
        logRepo.save(userService.createLog(u, "TOKEN_REFRESHED", ip, "Refreshed token"));
        return new AuthResponse(access, newRefresh, jwt.getAccessValiditySeconds());
    }

    public void logout(String refreshToken) {
        refreshRepo.findByToken(refreshToken).ifPresent(r -> {
            r.setRevoked(true);
            refreshRepo.save(r);
            logRepo.save(userService.createLog(r.getUser(), "LOGOUT", null, "Refresh token revoked"));
        });
    }
}

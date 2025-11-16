package com.example.auth.service;

import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.LoginRequest;
import com.example.auth.entity.RefreshToken;
import com.example.auth.entity.User;
import com.example.auth.repository.RefreshTokenRepository;
import com.example.auth.repository.UserLogRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock UserRepository userRepo;
    @Mock RefreshTokenRepository refreshRepo;
    @Mock JwtProvider jwt;
    @Mock BCryptPasswordEncoder encoder;
    @Mock UserService userService;
    @Mock UserLogRepository logRepo;

    @InjectMocks AuthService authService;

    @BeforeEach
    void setup() { MockitoAnnotations.openMocks(this); }

    @Test
    void login_userNotFound_throws() {
        when(userRepo.findByUsername("nope")).thenReturn(Optional.empty());
        LoginRequest req = new LoginRequest();
        req.setUsername("nope");
        req.setPassword("x");
        req.setCaptcha("c");
        assertThatThrownBy(() -> authService.login(req, "1.2.3.4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_accountNotEnabled_throws() {
        User u = User.builder().username("u").passwordHash("ph").enabled(false).locked(false).build();
        when(userRepo.findByUsername("u")).thenReturn(Optional.of(u));
        LoginRequest req = new LoginRequest();
        req.setUsername("u"); req.setPassword("pass"); req.setCaptcha("c");
        assertThatThrownBy(() -> authService.login(req, "ip"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account not confirmed");
    }

    @Test
    void login_accountLocked_throws() {
        User u = User.builder().username("u").passwordHash("ph").enabled(true).locked(true).build();
        when(userRepo.findByUsername("u")).thenReturn(Optional.of(u));
        LoginRequest req = new LoginRequest();
        req.setUsername("u"); req.setPassword("pass"); req.setCaptcha("c");
        assertThatThrownBy(() -> authService.login(req, "ip"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account locked");
    }

    @Test
    void login_badPassword_registersFailedAndThrows() {
        User u = User.builder().username("u").passwordHash("hash").enabled(true).locked(false).build();
        when(userRepo.findByUsername("u")).thenReturn(Optional.of(u));
        when(encoder.matches(anyString(), anyString())).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setUsername("u"); req.setPassword("wrong"); req.setCaptcha("c");

        assertThatThrownBy(() -> authService.login(req, "ip"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");

        verify(userService).registerFailedLogin(u, "ip");
    }

    @Test
    void login_success_savesRefreshAndReturnsTokens() {
        User u = User.builder().username("u").passwordHash("hash").enabled(true).locked(false).build();
        when(userRepo.findByUsername("u")).thenReturn(Optional.of(u));
        when(encoder.matches(eq("right"), anyString())).thenReturn(true);
        when(jwt.getAccessValiditySeconds()).thenReturn(60L);
        when(jwt.generateAccessToken("u")).thenReturn("access");
        when(jwt.generateRefreshToken("u")).thenReturn("refresh");

        when(refreshRepo.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoginRequest req = new LoginRequest();
        req.setUsername("u"); req.setPassword("right"); req.setCaptcha("c");

        AuthResponse resp = authService.login(req, "1.1.1.1");
        assertThat(resp).isNotNull();
        assertThat(resp.getAccessToken()).isEqualTo("access");
        assertThat(resp.getRefreshToken()).isEqualTo("refresh");
        assertThat(resp.getExpiresIn()).isEqualTo(60L);

        verify(refreshRepo).save(any(RefreshToken.class));
        verify(userService).resetFailedAttempts(u);
        verify(logRepo).save(any());
    }

    @Test
    void refresh_invalidRefresh_throws() {
        when(refreshRepo.findByToken("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.refresh("bad", "ip"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refresh_revokedOrExpired_throws() {
        RefreshToken r = RefreshToken.builder().token("t").revoked(true).expiryDate(Instant.now().plusSeconds(60)).build();
        when(refreshRepo.findByToken("t")).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> authService.refresh("t", "ip"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refresh token invalid or expired");
    }

    @Test
    void refresh_success_rotatesAndReturns() {
        User u = User.builder().username("u").build();
        RefreshToken r = RefreshToken.builder().token("t").revoked(false).expiryDate(Instant.now().plusSeconds(100)).user(u).build();
        when(refreshRepo.findByToken("t")).thenReturn(Optional.of(r));
        when(jwt.getUsernameFromToken("t")).thenReturn("u");
        when(jwt.generateAccessToken("u")).thenReturn("acc");
        when(jwt.generateRefreshToken("u")).thenReturn("newref");
        when(jwt.getAccessValiditySeconds()).thenReturn(123L);
        when(refreshRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse res = authService.refresh("t", "ip");
        assertThat(res.getAccessToken()).isEqualTo("acc");
        assertThat(res.getRefreshToken()).isEqualTo("newref");
        assertThat(res.getExpiresIn()).isEqualTo(123L);
        verify(refreshRepo).save(r);
        verify(logRepo).save(any());
    }

    @Test
    void logout_revokesIfPresent() {
        User u = User.builder().username("u").build();
        RefreshToken r = RefreshToken.builder().token("t").user(u).revoked(false).build();
        when(refreshRepo.findByToken("t")).thenReturn(Optional.of(r));
        doAnswer(inv -> {
            // simulate save
            return null;
        }).when(refreshRepo).save(any());

        authService.logout("t");
        assertThat(r.isRevoked()).isTrue();
        verify(refreshRepo).save(r);
        verify(logRepo).save(any());
    }
}

package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.service.AuthService;
import com.example.auth.service.CaptchaService;
import com.example.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock UserService userService;
    @Mock AuthService authService;
    @Mock CaptchaService captchaService;
    @Mock HttpServletRequest servlet;

    @InjectMocks AuthController controller;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void register_invalidCaptcha_returnsBadRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setCaptcha("x");
        when(captchaService.verify("x")).thenReturn(false);
        ResponseEntity<?> r = controller.register(req, servlet);
        assertThat(r.getStatusCodeValue()).isEqualTo(400);
        assertThat(r.getBody()).isEqualTo("Invalid CAPTCHA");
        verifyNoInteractions(userService);
    }

    @Test
    void register_valid_callsUserService() {
        RegisterRequest req = new RegisterRequest();
        req.setCaptcha("ok"); req.setUsername("u"); req.setPassword("p"); req.setEmail("e@test");
        when(captchaService.verify("ok")).thenReturn(true);
        when(servlet.getScheme()).thenReturn("http");
        when(servlet.getServerName()).thenReturn("localhost");
        when(servlet.getServerPort()).thenReturn(8081);
        ResponseEntity<?> r = controller.register(req, servlet);
        assertThat(r.getStatusCodeValue()).isEqualTo(200);
        verify(userService).register(eq(req), contains("http://localhost:8081"), eq(24));
    }

    @Test
    void login_invalidCaptcha_returns400() {
        LoginRequest req = new LoginRequest();
        req.setCaptcha("x");
        when(captchaService.verify("x")).thenReturn(false);
        ResponseEntity<?> r = controller.login(req, servlet);
        assertThat(r.getStatusCodeValue()).isEqualTo(400);
        assertThat(r.getBody()).isEqualTo("Invalid CAPTCHA");
        verifyNoInteractions(authService);
    }

    @Test
    void login_valid_callsAuthService() {
        LoginRequest req = new LoginRequest();
        req.setUsername("u"); req.setPassword("p"); req.setCaptcha("ok");
        when(captchaService.verify("ok")).thenReturn(true);
        when(servlet.getRemoteAddr()).thenReturn("1.2.3.4");
        when(authService.login(req, "1.2.3.4")).thenReturn(new AuthResponse("a","r",10));
        ResponseEntity<?> r = controller.login(req, servlet);
        assertThat(r.getStatusCodeValue()).isEqualTo(200);
        AuthResponse ar = (AuthResponse) r.getBody();
        assertThat(ar.getAccessToken()).isEqualTo("a");
    }

    @Test
    void refresh_callsAuthService() {
        when(servlet.getRemoteAddr()).thenReturn("ip");
        when(authService.refresh("ref","ip")).thenReturn(new AuthResponse("a","b",5L));
        AuthResponse res = controller.refresh("ref", servlet);
        assertThat(res.getAccessToken()).isEqualTo("a");
    }

    @Test
    void logout_callsAuthService_andReturnsString() {
        String out = controller.logout("refTok");
        verify(authService).logout("refTok");
        assertThat(out).isEqualTo("Logged out");
    }

    @Test
    void unlockRequest_callsUserService() {
        when(servlet.getRequestURL()).thenReturn(new StringBuffer("http://app/api/auth/unlock/request"));
        when(servlet.getRequestURI()).thenReturn("/api/auth/unlock/request");
        ResponseEntity<String> r = controller.unlockRequest("user1", servlet);
        assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
        verify(userService).sendUnlockToken(eq("user1"), anyString(), eq(24));
    }

    @Test
    void confirmUnlock_proxyToService() {
        when(userService.confirmUnlock("tok")).thenReturn("ok");
        ResponseEntity<String> r = controller.confirmUnlock("tok");
        assertThat(r.getBody()).isEqualTo("ok");
    }

    @Test
    void deleteAccount_callsDelete() {
        ResponseEntity<?> r = controller.deleteAccount("user1");
        verify(userService).deleteUser("user1");
        assertThat(r.getBody()).isEqualTo("Account deleted");
    }

    @Test
    void updateProfile_callsUpdateEmail() {
        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setEmail("n@test");
        org.springframework.security.core.userdetails.UserDetails ud =
                org.springframework.security.core.userdetails.User.withUsername("me").password("p").roles("CUSTOMER").build();
        ResponseEntity<?> r = controller.updateProfile(ud, req);
        verify(userService).updateEmail("me", "n@test");
        assertThat(r.getBody()).isEqualTo("Profile updated");
    }

    @Test
    void forgotPassword_callsService() {
        ResponseEntity<String> r = controller.forgotPassword("e@test");
        verify(userService).sendPasswordResetLink("e@test");
        assertThat(r.getBody()).contains("Password reset link sent");
    }

    @Test
    void resetPassword_callsService() {
        ResponseEntity<String> r = controller.resetPassword("tok", "newpass");
        verify(userService).resetPassword("tok", "newpass");
        assertThat(r.getBody()).contains("Password successfully reset");
    }
}

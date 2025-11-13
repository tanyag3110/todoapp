package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.service.AuthService;
import com.example.auth.service.CaptchaService;
import com.example.auth.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final AuthService authService;
    private final CaptchaService captchaService;

    public AuthController(UserService userService, AuthService authService, CaptchaService captchaService) {
        this.userService = userService;
        this.authService = authService;
        this.captchaService = captchaService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest servlet) {
        if (!captchaService.verify(req.getCaptcha())) {
            return ResponseEntity.badRequest().body("Invalid CAPTCHA");
        }
        String appUrl = getAppUrl(servlet);
        userService.register(req, appUrl, 24);
        return ResponseEntity.ok("Registration initiated. Check email to confirm.");
    }

    @GetMapping("/confirm")
    public String confirm(@RequestParam("token") String token) {
        return userService.confirmRegistration(token);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletRequest servlet) {
        if (!captchaService.verify(req.getCaptcha())) {
            return ResponseEntity.badRequest().body("Invalid CAPTCHA");
        }
        String ip = servlet.getRemoteAddr();
        return ResponseEntity.ok(authService.login(req, ip));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestParam("refreshToken") String refreshToken, HttpServletRequest servlet) {
        String ip = servlet.getRemoteAddr();
        return authService.refresh(refreshToken, ip);
    }

    @PostMapping("/logout")
    public String logout(@RequestParam("refreshToken") String refreshToken) {
        authService.logout(refreshToken);
        return "Logged out";
    }

    private String getAppUrl(HttpServletRequest req) {
        return req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort();
    }

    @PostMapping("/unlock/request")
    public ResponseEntity<String> unlockRequest(@RequestParam String username,
                                                HttpServletRequest request) {
        String appUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
        userService.sendUnlockToken(username, appUrl, 24);
        return ResponseEntity.ok("If the account exists and is locked, unlock email sent");
    }

    @GetMapping("/unlock/confirm")
    public ResponseEntity<String> confirmUnlock(@RequestParam String token) {
        return ResponseEntity.ok(userService.confirmUnlock(token));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal String username) {
        userService.deleteUser(username);
        return ResponseEntity.ok("Account deleted");
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails user,
                                           @RequestBody ProfileUpdateRequest req) {
        userService.updateEmail(user.getUsername(), req.getEmail());
        return ResponseEntity.ok("Profile updated");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        userService.sendPasswordResetLink(email);
        return ResponseEntity.ok("Password reset link sent (check mail logs)");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String token,
                                                @RequestParam String newPassword) {
        userService.resetPassword(token, newPassword);
        return ResponseEntity.ok("Password successfully reset. You can now login.");
    }
}

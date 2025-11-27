package com.example.auth.service;

import com.example.auth.dto.RegisterRequest;
import com.example.auth.entity.*;
import com.example.auth.repository.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepo;
    private final VerificationTokenRepository tokenRepo;
    private final EmailService emailService;
    private final PasswordEncoder encoder;
    private final UserLogRepository logRepo;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public UserService(UserRepository userRepo,
                       VerificationTokenRepository tokenRepo,
                       EmailService emailService,
                       BCryptPasswordEncoder encoder,
                       UserLogRepository logRepo, PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.emailService = emailService;
        this.encoder = encoder;
        this.logRepo = logRepo;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Transactional
    public void register(RegisterRequest req, String appUrl, int tokenValidityHours) {
        if (userRepo.findByUsername(req.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(encoder.encode(req.getPassword()))
                .role(req.getRole() == null ? "CUSTOMER" : req.getRole())
                .enabled(false)
                .locked(false)
                .createdAt(Instant.now())
                .build();
        userRepo.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken v = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plus(tokenValidityHours, ChronoUnit.HOURS))
                .build();
        tokenRepo.save(v);

        String link = appUrl + "/api/auth/confirm?token=" + token;
        String body = "<p>Thanks for registering. Confirm your account within " + tokenValidityHours + " hours:</p>"
                + "<p><a href=\"" + link + "\">Confirm account</a></p>";
        emailService.sendEmail(user.getEmail(), "Confirm your account", body);
        System.out.println("DEV CONFIRM LINK: " + link);


        logRepo.save(createLog(user, "REGISTER", null, "Registration initiated"));
    }

    public UserLog createLog(User user, String action, String ip, String details) {
        UserLog l = new UserLog();
        l.setUser(user);
        l.setAction(action);
        l.setIpAddress(ip);
        l.setDetails(details);
        return l;
    }

    @Transactional
    public String confirmRegistration(String token) {
        var opt = tokenRepo.findByToken(token);
        if (opt.isEmpty()) throw new IllegalArgumentException("Invalid token");
        var v = opt.get();
        if (v.getExpiryDate().isBefore(Instant.now())) {
            tokenRepo.delete(v);
            userRepo.delete(v.getUser());
            throw new IllegalArgumentException("Token expired. Please register again.");
        }
        User u = v.getUser();
        u.setEnabled(true);
        userRepo.save(u);
        tokenRepo.delete(v);
        logRepo.save(createLog(u, "CONFIRM", null, "Account confirmed"));
        return "confirmed";
    }

    @Transactional
    public void registerFailedLogin(User user, String ip) {
        user.setFailedLoginCount(user.getFailedLoginCount() + 1);
        if (user.getFailedLoginCount() >= 3) {
            user.setLocked(true);
            String body = "Your account is locked due to multiple failed attempts. Please re-register to unlock or contact support.";
            emailService.sendEmail(user.getEmail(), "Account locked", body);
            logRepo.save(createLog(user, "LOCK", ip, "Locked after failed attempts"));
        } else {
            logRepo.save(createLog(user, "LOGIN_FAIL", ip, "Failed login attempt"));
        }
        userRepo.save(user);
    }

    @Transactional
    public void resetFailedAttempts(User user) {
        user.setFailedLoginCount(0);
        userRepo.save(user);
    }

    @Transactional
    public void deleteUser(String username) {
        userRepo.findByUsername(username).ifPresent(userRepo::delete);
    }

    @Transactional
    public void sendUnlockToken(String username, String appUrl, int tokenValidityHours) {
        var opt = userRepo.findByUsername(username);
        if (opt.isEmpty()) return; // avoid username enumeration

        User user = opt.get();

        if (!user.isLocked()) return; // do nothing if not locked

        String token = UUID.randomUUID().toString();
        VerificationToken v = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plus(tokenValidityHours, ChronoUnit.HOURS))
                .build();
        tokenRepo.save(v);

        String link = appUrl + "/api/auth/unlock/confirm?token=" + token;
        String body = "<p>Your account is locked due to failed login attempts.</p>"
                + "<p>Click to unlock:</p>"
                + "<p><a href=\"" + link + "\">Unlock Account</a></p>";

        emailService.sendEmail(user.getEmail(), "Unlock Your Account", body);
        logRepo.save(createLog(user, "UNLOCK_REQUEST", null, "User requested unlock email"));
    }

    @Transactional
    public String confirmUnlock(String token) {
        var opt = tokenRepo.findByToken(token);
        if (opt.isEmpty()) throw new IllegalArgumentException("Invalid token");

        var v = opt.get();
        if (v.getExpiryDate().isBefore(Instant.now())) {
            tokenRepo.delete(v);
            throw new IllegalArgumentException("Token expired. Request unlock again.");
        }

        User user = v.getUser();
        user.setLocked(false);
        user.setFailedLoginCount(0);
        userRepo.save(user);
        tokenRepo.delete(v);

        logRepo.save(createLog(user, "UNLOCK", null, "Account unlocked"));
        return "Account unlocked. You may now login.";
    }

    @Transactional
    public void updateEmail(String username, String newEmail) {
        var userOpt = userRepo.findByUsername(username);
        if (userOpt.isEmpty()) throw new IllegalArgumentException("User not found");
        User user = userOpt.get();

        // Check if email already exists
        if (userRepo.findByEmail(newEmail).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        user.setEmail(newEmail);
        user.setEnabled(false); // Require reconfirmation after email change
        userRepo.save(user);

        // Generate new verification token
        String token = UUID.randomUUID().toString();
        VerificationToken ver = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiryDate(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        tokenRepo.save(ver);

        String link = "https://listtodo.duckdns.org/api/auth/confirm?token=" + token;
        String body = "<p>Your email has been updated. Confirm new email:</p>"
                + "<p><a href=\"" + link + "\">Confirm</a></p>";

        emailService.sendEmail(newEmail, "Confirm New Email", body);

        logRepo.save(createLog(user, "EMAIL_UPDATE", null, "Email updated, reconfirmation required"));
    }

    @Transactional
    public void sendPasswordResetLink(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No user registered with this email"));

        passwordResetTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();

        PasswordResetToken prt = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiry(LocalDateTime.now().plusMinutes(30))
                .build();

        passwordResetTokenRepository.save(prt);

        String resetUrl = "https://listtodo.duckdns.org/reset/" + token;

        emailService.sendEmail(user.getEmail(), "Password Reset Instructions",
                "Click the link below to reset your password:\n\n" + resetUrl);

        System.out.println("RESET LINK SENT -> " + resetUrl);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        if (prt.getExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token expired");
        }

        User user = prt.getUser();
        user.setPasswordHash(encoder.encode(newPassword));
        userRepo.save(user);

        // Delete token so it can't be reused
        passwordResetTokenRepository.delete(prt);
    }
}

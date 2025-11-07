package com.example.auth.service;

import com.example.auth.dto.RegisterRequest;
import com.example.auth.entity.*;
import com.example.auth.repository.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepo;
    private final VerificationTokenRepository tokenRepo;
    private final EmailService emailService;
    private final BCryptPasswordEncoder encoder;
    private final UserLogRepository logRepo;

    public UserService(UserRepository userRepo,
                       VerificationTokenRepository tokenRepo,
                       EmailService emailService,
                       BCryptPasswordEncoder encoder,
                       UserLogRepository logRepo) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.emailService = emailService;
        this.encoder = encoder;
        this.logRepo = logRepo;
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
            String body = "<p>Your account is locked due to multiple failed attempts. Please re-register to unlock or contact support.</p>";
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
    public void deleteUser(User user) {
        userRepo.delete(user);
        logRepo.save(createLog(user, "DEREGISTER", null, "User deleted"));
    }
}

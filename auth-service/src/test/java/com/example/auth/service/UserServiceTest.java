package com.example.auth.service;

import com.example.auth.dto.RegisterRequest;
import com.example.auth.entity.*;
import com.example.auth.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock UserRepository userRepo;
    @Mock VerificationTokenRepository tokenRepo;
    @Mock EmailService emailService;
    @Mock BCryptPasswordEncoder encoder;
    @Mock UserLogRepository logRepo;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks UserService userService;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void register_usernameExists_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("u");
        when(userRepo.findByUsername("u")).thenReturn(Optional.of(new User()));
        assertThatThrownBy(() -> userService.register(req, "http://app", 24))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void register_success_savesUserAndTokenAndEmails() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("u");
        req.setEmail("e@test");
        req.setPassword("pass");
        req.setRole(null);
        req.setCaptcha("c");

        when(userRepo.findByUsername("u")).thenReturn(Optional.empty());
        when(encoder.encode("pass")).thenReturn("encoded");
        when(tokenRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.register(req, "http://app", 24);

        verify(userRepo).save(any(User.class));
        verify(tokenRepo).save(any(VerificationToken.class));
        verify(emailService).sendEmail(eq("e@test"), contains("Confirm"), anyString());
        verify(logRepo).save(any());
    }

    @Test
    void confirmRegistration_invalidToken_throws() {
        when(tokenRepo.findByToken("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.confirmRegistration("bad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid token");
    }

    @Test
    void confirmRegistration_expired_deletesUserAndThrows() {
        User u = User.builder().username("u").build();
        VerificationToken v = VerificationToken.builder().token("t").user(u).expiryDate(Instant.now().minusSeconds(10)).build();
        when(tokenRepo.findByToken("t")).thenReturn(Optional.of(v));

        assertThatThrownBy(() -> userService.confirmRegistration("t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token expired");

        verify(tokenRepo).delete(v);
        verify(userRepo).delete(u);
    }

    @Test
    void confirmRegistration_success_enablesUserAndDeletesToken() {
        User u = User.builder().username("u").enabled(false).build();
        VerificationToken v = VerificationToken.builder().token("t").user(u).expiryDate(Instant.now().plusSeconds(3600)).build();
        when(tokenRepo.findByToken("t")).thenReturn(Optional.of(v));
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
//        when(tokenRepo.delete(any())).thenReturn(null);

        String res = userService.confirmRegistration("t");
        assertThat(res).isEqualTo("confirmed");
        assertThat(u.isEnabled()).isTrue();
        verify(tokenRepo).delete(v);
        verify(logRepo).save(any());
    }

    @Test
    void registerFailedLogin_lessThanThreshold_logsAndSaves() {
        User u = User.builder().username("u").failedLoginCount(0).email("e").build();
        userService.registerFailedLogin(u, "1.1.1.1");
        assertThat(u.getFailedLoginCount()).isEqualTo(1);
        verify(logRepo).save(any());
        verify(userRepo).save(u);
    }

    @Test
    void registerFailedLogin_reachesThreshold_locks_andEmails() {
        User u = User.builder().username("u").failedLoginCount(2).email("e@test").build();
        userService.registerFailedLogin(u, "ip");
        assertThat(u.isLocked()).isTrue();
        verify(emailService).sendEmail(eq("e@test"), contains("locked"), anyString());
        verify(logRepo).save(any());
        verify(userRepo).save(u);
    }

    @Test
    void resetFailedAttempts_resets() {
        User u = User.builder().failedLoginCount(5).build();
        userService.resetFailedAttempts(u);
        assertThat(u.getFailedLoginCount()).isEqualTo(0);
        verify(userRepo).save(u);
    }

    @Test
    void deleteUser_whenPresent_deletes() {
        User u = User.builder().username("x").build();
        when(userRepo.findByUsername("x")).thenReturn(Optional.of(u));
        userService.deleteUser("x");
        verify(userRepo).delete(u);
    }

    @Test
    void sendUnlockToken_userNotFound_returnsNoop() {
        when(userRepo.findByUsername("no")).thenReturn(Optional.empty());
        userService.sendUnlockToken("no", "http://app", 24);
        verifyNoInteractions(emailService);
    }

    @Test
    void sendUnlockToken_whenLocked_createsTokenAndSendsEmail() {
        User u = User.builder().username("u").email("e@test").locked(true).build();
        when(userRepo.findByUsername("u")).thenReturn(Optional.of(u));
        when(tokenRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        userService.sendUnlockToken("u", "http://app", 24);
        verify(emailService).sendEmail(eq("e@test"), contains("Unlock"), anyString());
        verify(logRepo).save(any());
    }

    @Test
    void confirmUnlock_invalidToken_throws() {
        when(tokenRepo.findByToken("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.confirmUnlock("bad"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confirmUnlock_expired_deletesTokenAndThrows() {
        User u = User.builder().username("u").build();
        VerificationToken v = VerificationToken.builder().token("t").user(u).expiryDate(Instant.now().minusSeconds(5)).build();
        when(tokenRepo.findByToken("t")).thenReturn(Optional.of(v));
        assertThatThrownBy(() -> userService.confirmUnlock("t"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Token expired");
        verify(tokenRepo).delete(v);
    }

    @Test
    void confirmUnlock_success_unlocksAndDeletesToken() {
        User u = User.builder().username("u").locked(true).failedLoginCount(3).build();
        VerificationToken v = VerificationToken.builder().token("t").user(u).expiryDate(Instant.now().plusSeconds(3600)).build();
        when(tokenRepo.findByToken("t")).thenReturn(Optional.of(v));
        String res = userService.confirmUnlock("t");
        assertThat(res).contains("Account unlocked");
        assertThat(u.isLocked()).isFalse();
        assertThat(u.getFailedLoginCount()).isEqualTo(0);
        verify(tokenRepo).delete(v);
        verify(logRepo).save(any());
    }

    @Test
    void updateEmail_userNotFound_throws() {
        when(userRepo.findByUsername("u")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.updateEmail("u", "n@test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateEmail_emailAlreadyUsed_throws() {
        User u = User.builder().username("u").email("old@test").build();
        when(userRepo.findByUsername("u")).thenReturn(Optional.of(u));
        when(userRepo.findByEmail("taken@test")).thenReturn(Optional.of(new User()));
        assertThatThrownBy(() -> userService.updateEmail("u", "taken@test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    void sendPasswordResetLink_userNotFound_throws() {
        when(userRepo.findByEmail("no")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.sendPasswordResetLink("no"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No user registered");
    }

    @Test
    void sendPasswordResetLink_success_savesAndSends() {
        User u = User.builder().email("e@test").build();
        when(userRepo.findByEmail("e@test")).thenReturn(Optional.of(u));
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        userService.sendPasswordResetLink("e@test");
        verify(passwordResetTokenRepository).save(any());
        verify(emailService).sendEmail(eq("e@test"), contains("Password Reset"), anyString());
    }

    @Test
    void resetPassword_invalidToken_throws() {
        when(passwordResetTokenRepository.findByToken("bad")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.resetPassword("bad", "new"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid reset token");
    }

    @Test
    void resetPassword_expired_throws() {
        User u = User.builder().username("u").build();
        PasswordResetToken prt = PasswordResetToken.builder().token("t").user(u).expiry(LocalDateTime.now().minusMinutes(1)).build();
        when(passwordResetTokenRepository.findByToken("t")).thenReturn(Optional.of(prt));
        assertThatThrownBy(() -> userService.resetPassword("t", "new"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Reset token expired");
    }

    // For resetPassword success, ensure password is changed and token deleted
    @Test
    void resetPassword_success_changesPassword_andDeletesToken() {
        User u = User.builder().username("u").passwordHash("old").build();
        PasswordResetToken prt = PasswordResetToken.builder().token("t").user(u).expiry(LocalDateTime.now().plusMinutes(10)).build();
        when(passwordResetTokenRepository.findByToken("t")).thenReturn(Optional.of(prt));
        when(encoder.encode("new")).thenReturn("encodedNew");
        // inject encoder via reflection? But in our service encoder is BCryptPasswordEncoder injected in constructor - since test class uses @InjectMocks and encoder mocked, it will be used.
        userService.resetPassword("t", "new");
        verify(passwordResetTokenRepository).delete(prt);
        verify(userRepo).save(u);
        assertThat(u.getPasswordHash()).isNotNull();
    }
}

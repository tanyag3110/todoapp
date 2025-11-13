package com.example.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username; // email for customers/internal; appId for applications

    @Column(nullable = false)
    private String passwordHash;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String role;

    private boolean enabled;
    private boolean locked;

    private int failedLoginCount;
    private Instant lastFailedAttempt;

    private Instant createdAt = Instant.now();
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        failedLoginCount = 0;
        locked = false;
        enabled = false;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

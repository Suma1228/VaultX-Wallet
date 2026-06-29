package com.wallet.auth_service.entity;

import com.wallet.auth_service.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_phone", columnList = "phone_number"),   // ← snake_case
                @Index(name = "idx_user_userId", columnList = "user_id")        // ← snake_case
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===================== IDENTIFIERS =====================
    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private String userId;
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // ===================== PERSONAL INFO =====================
    private String firstName;
    private String lastName;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    private LocalDate dateOfBirth;

    // ===================== ADDRESS =====================
    private String address;
    private String city;
    private String state;
    private String country;
    private String pinCode;

    // ===================== ACCOUNT STATUS =====================
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;

    private boolean emailVerified;
    private boolean phoneVerified;

    // ===================== PASSWORD MANAGEMENT =====================
    private String passwordResetToken;
    private LocalDateTime passwordResetTokenExpiry;

    private LocalDateTime passwordChangedAt;

    // ===================== ACCOUNT CONTROL =====================
    private String suspensionReason;
    private LocalDateTime suspendedAt;

    // ===================== AUDIT FIELDS =====================
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;

    // ===================== LIFECYCLE CALLBACKS =====================
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.accountStatus == null) {
            this.accountStatus = AccountStatus.ACTIVE;
        }

        this.emailVerified = false;
        this.phoneVerified = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
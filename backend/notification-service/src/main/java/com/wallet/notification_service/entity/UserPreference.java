package com.wallet.notification_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores notification preferences per user.
 * Populated when user registers (via Kafka user-events topic)
 * or via the REST API.
 */
@Entity
@Table(name = "user_preferences",
        indexes = {
                @Index(name = "idx_up_user_id", columnList = "userId", unique = true)
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    private String email;
    private String phoneNumber;

    // ── opt-in flags ──────────────────────────────────────────────
    @Builder.Default
    private boolean emailEnabled = true;

    @Builder.Default
    private boolean smsEnabled = false;    // SMS gateway not wired yet

    @Builder.Default
    private boolean creditAlerts = true;

    @Builder.Default
    private boolean debitAlerts = true;

    @Builder.Default
    private boolean transferAlerts = true;

    @Builder.Default
    private boolean securityAlerts = true; // freeze/suspend

    @Builder.Default
    private boolean spendAlerts = true;    // daily limit / low balance

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

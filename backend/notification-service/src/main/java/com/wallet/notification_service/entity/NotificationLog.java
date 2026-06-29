package com.wallet.notification_service.entity;

import com.wallet.notification_service.enums.NotificationStatus;
import com.wallet.notification_service.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persists a record for every notification processed.
 * Used for audit trail, retry logic, and reporting.
 */
@Entity
@Table(name = "notification_logs",
        indexes = {
                @Index(name = "idx_nl_user_id", columnList = "userId"),
                @Index(name = "idx_nl_event_type", columnList = "eventType"),
                @Index(name = "idx_nl_status", columnList = "status"),
                @Index(name = "idx_nl_created_at", columnList = "createdAt")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String notificationId;     // UUID

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String eventType;          // mirrors WalletEvent.eventType

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private String recipient;          // email address / phone number

    @Column(columnDefinition = "TEXT")
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String messageBody;

    // ── linked transaction details ─────────────────────────────────
    private String transactionId;
    private String walletId;
    private BigDecimal amount;
    private BigDecimal balanceAfter;

    // ── failure info ───────────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String failureReason;

    private int retryCount;

    // ── timestamps ────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime updatedAt;
}

package com.wallet.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Deserialized from Kafka topic: wallet-events
 * Produced by wallet-service whenever a transaction occurs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalletEvent {

    // ── who / what ──────────────────────────────────────────────────
    private String eventId;
    private String eventType;          // MONEY_ADDED, MONEY_WITHDRAWN, MONEY_TRANSFERRED_DEBIT, MONEY_TRANSFERRED_CREDIT, WALLET_FROZEN, WALLET_SUSPENDED, DAILY_LIMIT_EXCEEDED, LOW_BALANCE_ALERT
    private String userId;
    private String walletId;

    // ── transaction details (nullable for non-tx events) ────────────
    private String transactionId;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String currency;
    private String description;
    private String status;             // SUCCESS, FAILED, PENDING

    // ── transfer-specific ────────────────────────────────────────────
    private String transferId;
    private String senderUserId;
    private String recipientUserId;

    private String senderName;
    private String recipientName;

    // ── wallet status events ─────────────────────────────────────────
    private String reason;             // freeze/suspend reason

    // ── alert-specific ───────────────────────────────────────────────
    private BigDecimal dailyLimit;
    private BigDecimal dailySpent;
    private BigDecimal alertThreshold;

    // ── metadata ─────────────────────────────────────────────────────
    private LocalDateTime occurredAt;
    private String serviceSource;      // "wallet-service"
}

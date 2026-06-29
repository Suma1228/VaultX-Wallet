package com.wallet.wallet_service.entity;

import com.wallet.wallet_service.enums.TransactionStatus;
import com.wallet.wallet_service.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_transaction_id", columnList = "transaction_id"),
                @Index(name = "idx_wallet_id", columnList = "wallet_id"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_type", columnList = "type"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_transfer_id", columnList = "transfer_id"),
                @Index(name = "idx_created_at", columnList = "created_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "transaction_id", unique = true, nullable = false, length = 50)
    private String transactionId;
    @Column(name = "wallet_id", nullable = false, length = 50)
    private String walletId;
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TransactionType type;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;
    @Column(name = "description", length = 500)
    private String description;
    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;
    // For linking transfer debit and credit transactions
    @Column(name = "transfer_id", length = 50)
    private String transferId;
    // For transfer credit transactions
    @Column(name = "sender_user_id", length = 50)
    private String senderUserId;

    // For transfer debit transactions
    @Column(name = "recipient_user_id", length = 50)
    private String recipientUserId;

    // UPI, Card, Net Banking, etc.
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    // External payment gateway reference
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    // For withdrawals
    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    // For withdrawals
    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}

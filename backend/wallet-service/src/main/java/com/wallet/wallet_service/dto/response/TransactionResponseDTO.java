package com.wallet.wallet_service.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {

    private String transactionId;
    private String walletId;
    private String userId;

    private BigDecimal amount;

    private String type;
    private String status;

    private String description;

    private BigDecimal balanceAfter;

    private String transferId;

    private String senderUserId;
    private String recipientUserId;

    private String paymentMethod;
    private String referenceId;

    private String bankAccountNumber;
    private String ifscCode;

    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
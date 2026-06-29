package com.wallet.wallet_service.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletEvent {

    private String eventType;

    private String userId;

    private String email;
    private String phoneNumber;
    private String transferId;

    private String walletId;

    private String recipientUserId;

    private String senderUserId;

    private String transactionId;

    private String senderName;
    private String recipientName;

    private BigDecimal amount;

    // NEW
    private BigDecimal balanceAfter;

    // NEW
    private String currency;
    private String reason;
    private LocalDateTime occurredAt;   // was "timestamp"
}
package com.wallet.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogDTO {

    private String notificationId;
    private String userId;
    private String eventType;
    private String notificationType;
    private String status;
    private String recipient;
    private String subject;
    private String transactionId;
    private String walletId;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String failureReason;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}

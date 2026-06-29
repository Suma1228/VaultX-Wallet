package com.wallet.wallet_service.dto.response;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@   Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponseDTO {

    private String walletId;
    private String userId;

    private BigDecimal balance;
    private String currency;
    private String status;

    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;

    private BigDecimal dailySpent;
    private BigDecimal monthlySpent;

    private String freezeReason;
    private LocalDateTime frozenAt;

    private String suspensionReason;
    private LocalDateTime suspendedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
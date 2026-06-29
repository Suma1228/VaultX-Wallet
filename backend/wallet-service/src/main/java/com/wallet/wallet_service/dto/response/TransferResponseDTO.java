package com.wallet.wallet_service.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponseDTO {

    private String transferId;

    private String fromUserId;
    private String toUserId;

    private BigDecimal amount;

    private String status;

    private BigDecimal senderBalanceAfter;

    private LocalDateTime transferredAt;
}
package com.wallet.wallet_service.dto.response;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponseDTO {

    private String walletId;
    private String userId;

    private BigDecimal balance;
    private String currency;
    private String status;

    private LocalDateTime lastUpdated;
}
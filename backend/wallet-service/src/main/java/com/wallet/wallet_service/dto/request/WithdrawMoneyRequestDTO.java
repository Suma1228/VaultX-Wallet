package com.wallet.wallet_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawMoneyRequestDTO {

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private String description;

    private String bankAccountNumber;

    private String ifscCode;
}
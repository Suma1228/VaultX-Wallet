package com.wallet.wallet_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLimitRequestDTO {

    @NotNull
    private BigDecimal limit;
}
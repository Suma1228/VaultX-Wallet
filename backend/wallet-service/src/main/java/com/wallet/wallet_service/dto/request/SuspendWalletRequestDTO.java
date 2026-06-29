package com.wallet.wallet_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspendWalletRequestDTO {

    @NotBlank
    private String reason;
}
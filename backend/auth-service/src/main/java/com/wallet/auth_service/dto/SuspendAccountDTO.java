package com.wallet.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuspendAccountDTO {

    @NotBlank(message = "Reason is required for suspension")
    private String reason;
}
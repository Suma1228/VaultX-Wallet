package com.wallet.auth_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountStatusResponseDTO {

    private String userId;
    private String status;
}
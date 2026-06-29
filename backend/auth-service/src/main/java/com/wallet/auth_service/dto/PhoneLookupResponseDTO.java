package com.wallet.auth_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PhoneLookupResponseDTO {
    private String userId;
    private String firstName;
    private String lastName;
}
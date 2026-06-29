package com.wallet.auth_service.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordDTO {

    private String token;
    private String newPassword;
}
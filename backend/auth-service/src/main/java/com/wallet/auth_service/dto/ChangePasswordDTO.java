package com.wallet.auth_service.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordDTO {

    private String currentPassword;
    private String newPassword;
}
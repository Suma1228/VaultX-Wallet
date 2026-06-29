package com.wallet.auth_service.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String token;
    private String tokenType; // "Bearer"

    private String userId;
    private String email;

    private String firstName;
    private String lastName;

    private String accountStatus;
}
package com.wallet.auth_service.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDTO {

    private String email;
    private String password;

    private String firstName;
    private String lastName;

    private String phoneNumber;
    private LocalDate dateOfBirth;
}
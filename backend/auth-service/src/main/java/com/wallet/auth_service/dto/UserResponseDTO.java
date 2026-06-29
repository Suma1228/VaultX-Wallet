package com.wallet.auth_service.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private String userId;
    private String email;

    private String firstName;
    private String lastName;

    private String phoneNumber;
    private LocalDate dateOfBirth;

    private String address;
    private String city;
    private String state;
    private String country;
    private String pinCode;

    private String accountStatus;

    private boolean emailVerified;
    private boolean phoneVerified;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
}
package com.wallet.auth_service.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDTO {

    private String firstName;
    private String lastName;

    private String phoneNumber;
    private LocalDate dateOfBirth;

    private String address;
    private String city;
    private String state;
    private String country;
    private String pinCode;
}
package com.wallet.auth_service.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEvent {

    private String eventType;

    private String userId;

    private String email;

    private String phoneNumber;

    private String firstName;

    private String message;

    private LocalDateTime timestamp;
}
package com.wallet.wallet_service.exception;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    private String message;
    private String errorCode;
    private int status;
    private LocalDateTime timestamp;
}
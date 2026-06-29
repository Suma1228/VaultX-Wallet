package com.wallet.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceDTO {

    @NotBlank(message = "userId is required")
    private String userId;

    @Email(message = "Invalid email format")
    private String email;

    private String phoneNumber;

    @Builder.Default
    private boolean emailEnabled = true;

    @Builder.Default
    private boolean smsEnabled = false;

    @Builder.Default
    private boolean creditAlerts = true;

    @Builder.Default
    private boolean debitAlerts = true;

    @Builder.Default
    private boolean transferAlerts = true;

    @Builder.Default
    private boolean securityAlerts = true;

    @Builder.Default
    private boolean spendAlerts = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

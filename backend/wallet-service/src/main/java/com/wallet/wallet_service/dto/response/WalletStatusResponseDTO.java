package com.wallet.wallet_service.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletStatusResponseDTO {

    private String userId;
    private String status;
}
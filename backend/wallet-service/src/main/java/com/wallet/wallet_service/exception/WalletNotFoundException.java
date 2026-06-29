package com.wallet.wallet_service.exception;

public class WalletNotFoundException extends WalletServiceException {
    public WalletNotFoundException(String message) {
        super(message);
    }
}
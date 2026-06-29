package com.wallet.wallet_service.exception;

public class WalletAlreadyExistsException extends WalletServiceException {
    public WalletAlreadyExistsException(String message) {
        super(message);
    }
}
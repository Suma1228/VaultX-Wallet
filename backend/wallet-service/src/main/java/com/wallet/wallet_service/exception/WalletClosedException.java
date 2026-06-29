package com.wallet.wallet_service.exception;

public class WalletClosedException extends WalletServiceException {
    public WalletClosedException(String message) {
        super(message);
    }
}
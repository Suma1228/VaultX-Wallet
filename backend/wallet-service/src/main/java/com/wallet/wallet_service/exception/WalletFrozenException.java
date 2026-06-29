package com.wallet.wallet_service.exception;

public class WalletFrozenException extends WalletServiceException {
    public WalletFrozenException(String message) {
        super(message);
    }
}
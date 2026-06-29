package com.wallet.wallet_service.exception;

public class WalletSuspendedException extends WalletServiceException {
    public WalletSuspendedException(String message) {
        super(message);
    }
}
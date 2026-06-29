package com.wallet.wallet_service.exception;

public class InsufficientBalanceException extends WalletServiceException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}

package com.wallet.wallet_service.exception;

public class TransactionNotFoundException extends WalletServiceException {
    public TransactionNotFoundException(String message) {
        super(message);
    }
}
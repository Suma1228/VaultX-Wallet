package com.wallet.wallet_service.exception;

public class TransactionFailedException extends WalletServiceException {
    public TransactionFailedException(String message) {
        super(message);
    }
}
package com.wallet.wallet_service.exception;

public class InvalidAmountException extends WalletServiceException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
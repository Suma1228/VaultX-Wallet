package com.wallet.wallet_service.exception;

public class InvalidTransferException extends WalletServiceException {
    public InvalidTransferException(String message) {
        super(message);
    }
}

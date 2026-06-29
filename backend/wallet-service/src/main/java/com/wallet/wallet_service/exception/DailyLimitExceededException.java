package com.wallet.wallet_service.exception;

public class DailyLimitExceededException extends WalletServiceException {
    public DailyLimitExceededException(String message) {
        super(message);
    }
}
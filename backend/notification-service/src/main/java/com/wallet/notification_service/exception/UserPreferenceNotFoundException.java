package com.wallet.notification_service.exception;

public class UserPreferenceNotFoundException extends RuntimeException {
    public UserPreferenceNotFoundException(String message) {
        super(message);
    }
}

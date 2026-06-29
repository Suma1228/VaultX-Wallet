package com.wallet.auth_service.exception;

import org.springframework.http.HttpStatus;

public class AccountSuspendedException extends BaseException {

    public AccountSuspendedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
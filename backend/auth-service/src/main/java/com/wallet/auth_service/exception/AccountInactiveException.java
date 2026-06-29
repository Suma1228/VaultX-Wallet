package com.wallet.auth_service.exception;
import org.springframework.http.HttpStatus;

public class AccountInactiveException extends BaseException {

    public AccountInactiveException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
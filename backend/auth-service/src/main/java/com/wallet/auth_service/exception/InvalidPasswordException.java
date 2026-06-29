package com.wallet.auth_service.exception;

import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends BaseException {

    public InvalidPasswordException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
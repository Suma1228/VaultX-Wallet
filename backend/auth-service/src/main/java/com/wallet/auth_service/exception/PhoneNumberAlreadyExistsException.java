package com.wallet.auth_service.exception;

import org.springframework.http.HttpStatus;

public class PhoneNumberAlreadyExistsException extends BaseException {

    public PhoneNumberAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
package com.wallet.wallet_service.enums;

public enum TransactionStatus {

    PENDING,    // Transaction initiated but not completed

    SUCCESS,    // Transaction completed successfully

    FAILED,     // Transaction failed

    CANCELLED,  // Transaction cancelled

    REVERSED    // Transaction reversed
}
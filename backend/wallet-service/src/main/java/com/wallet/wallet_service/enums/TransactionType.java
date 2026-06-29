package com.wallet.wallet_service.enums;

public enum TransactionType {

    CREDIT,             // Money added to wallet from external source

    DEBIT,              // Money withdrawn from wallet to external account

    TRANSFER_DEBIT,     // Money sent to another wallet (sender's transaction)

    TRANSFER_CREDIT,    // Money received from another wallet (receiver's transaction)

    REFUND,             // Refund transaction

    REVERSAL,           // Transaction reversal

    CASHBACK,           // Cashback credited

    REWARD,             // Reward points credited

    FEE,                // Service fee deducted

    PENALTY             // Penalty deducted
}
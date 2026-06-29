package com.wallet.wallet_service.enums;

public enum WalletStatus {

    ACTIVE,      // Wallet is active and can be used

    INACTIVE,    // Wallet is inactive (not verified or deactivated by user)

    FROZEN,      // Wallet is temporarily frozen (by admin or system)

    SUSPENDED,   // Wallet is suspended (by admin for violations)

    CLOSED       // Wallet is permanently closed
}
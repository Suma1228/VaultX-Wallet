package com.wallet.wallet_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.wallet.wallet_service.dto.request.AddMoneyRequestDTO;
import com.wallet.wallet_service.dto.request.TransferMoneyRequestDTO;
import com.wallet.wallet_service.dto.request.WithdrawMoneyRequestDTO;
import com.wallet.wallet_service.dto.response.BalanceResponseDTO;
import com.wallet.wallet_service.dto.response.TransactionResponseDTO;
import com.wallet.wallet_service.dto.response.TransferResponseDTO;
import com.wallet.wallet_service.dto.response.WalletResponseDTO;
import com.wallet.wallet_service.enums.WalletStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WalletService {

    // ==================== WALLET CREATION & MANAGEMENT ====================

    WalletResponseDTO createWallet(String userId);

    WalletResponseDTO getWalletByUserId(String userId);

    WalletResponseDTO getWalletById(String walletId);

    // ==================== BALANCE OPERATIONS ====================

    BalanceResponseDTO getBalance(String userId);

    WalletResponseDTO addMoney(String userId, AddMoneyRequestDTO addMoneyRequest);

    WalletResponseDTO withdrawMoney(String userId, WithdrawMoneyRequestDTO withdrawRequest);

    // ==================== TRANSFER OPERATIONS ====================

    TransferResponseDTO transferMoney(
            String fromUserId,
            TransferMoneyRequestDTO transferRequest);

    // ==================== TRANSACTION HISTORY ====================

    Page<TransactionResponseDTO> getTransactionHistory(
            String userId,
            Pageable pageable);

    TransactionResponseDTO getTransactionById(
            String userId,
            String transactionId);

    List<TransactionResponseDTO> getTransactionsByDateRange(
            String userId,
            LocalDateTime startDate,
            LocalDateTime endDate);

    // ==================== WALLET STATUS MANAGEMENT ====================

    void activateWallet(String userId);

    void freezeWallet(String userId, String reason);

    void suspendWallet(String userId, String reason);

    WalletStatus getWalletStatus(String userId);

    // ==================== LIMITS MANAGEMENT ====================

    void updateDailyLimit(String userId, BigDecimal newLimit);

    void updateMonthlyLimit(String userId, BigDecimal newLimit);

    void resetDailySpent(String userId);
}

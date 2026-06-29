package com.wallet.wallet_service.service;

import com.wallet.wallet_service.dto.request.AddMoneyRequestDTO;
import com.wallet.wallet_service.dto.request.TransferMoneyRequestDTO;
import com.wallet.wallet_service.dto.request.WithdrawMoneyRequestDTO;
import com.wallet.wallet_service.dto.response.BalanceResponseDTO;
import com.wallet.wallet_service.dto.response.TransactionResponseDTO;
import com.wallet.wallet_service.dto.response.TransferResponseDTO;
import com.wallet.wallet_service.dto.response.WalletResponseDTO;
import com.wallet.wallet_service.entity.Transaction;
import com.wallet.wallet_service.entity.Wallet;
import com.wallet.wallet_service.enums.TransactionStatus;
import com.wallet.wallet_service.enums.TransactionType;
import com.wallet.wallet_service.enums.WalletStatus;
import com.wallet.wallet_service.event.WalletEvent;
import com.wallet.wallet_service.exception.*;
import com.wallet.wallet_service.kafka.KafkaProducerService;
import com.wallet.wallet_service.repository.TransactionRepository;
import com.wallet.wallet_service.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j                          // Fix #1: was @Sif4j (typo)
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaProducerService kafkaProducerService;

    private static final BigDecimal MIN_BALANCE = BigDecimal.ZERO;
    private static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("100000");
    private static final BigDecimal INITIAL_BALANCE = BigDecimal.ZERO;

    // ==================== WALLET CREATION & MANAGEMENT ====================

    @Override
    public WalletResponseDTO createWallet(String userId) {
        log.info("Creating wallet for userId: {}", userId);

        // Fix #2: if-check must come BEFORE the throw, not after it
        if (walletRepository.existsByUserId(userId)) {
            throw new WalletAlreadyExistsException("Wallet already exists for user: " + userId);
        }

        Wallet wallet = Wallet.builder()
                .walletId(UUID.randomUUID().toString())
                .userId(userId)
                .balance(INITIAL_BALANCE)
                .currency("INR")
                .status(WalletStatus.ACTIVE)
                .dailyLimit(new BigDecimal("50000"))
                .monthlyLimit(new BigDecimal("200000"))
                .dailySpent(BigDecimal.ZERO)
                .monthlySpent(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Wallet savedWallet = walletRepository.save(wallet);
        kafkaProducerService.publishWalletEvent(
                WalletEvent.builder()
                        .eventType("WALLET_CREATED")
                        .userId(savedWallet.getUserId())
                        .walletId(savedWallet.getWalletId())
                        .balanceAfter(savedWallet.getBalance())
                        .currency("INR")
                        .occurredAt(LocalDateTime.now())
                        .build()
        );
        log.info("Wallet created successfully with walletId: {} for userId: {}", savedWallet.getWalletId(), userId);
        return mapToWalletResponseDTO(savedWallet);
    }

    @Override
    public WalletResponseDTO getWalletByUserId(String userId) {
        log.info("Fetching wallet for userId: {}", userId);
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        return mapToWalletResponseDTO(wallet);
    }

    @Override
    public WalletResponseDTO getWalletById(String walletId) {
        log.info("Fetching wallet by walletId: {}", walletId);
        Wallet wallet = walletRepository.findByWalletId(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));
        return mapToWalletResponseDTO(wallet);
    }

    // ==================== BALANCE OPERATIONS ====================

    @Override
    public BalanceResponseDTO getBalance(String userId) {
        log.info("Fetching balance for userId: {}", userId);
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));

        // Fix #3: lastUpdated() must be BEFORE build(), not after it
        return BalanceResponseDTO.builder()
                .walletId(wallet.getWalletId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus().toString())
                .lastUpdated(wallet.getUpdatedAt())   // was placed after build() — unreachable
                .build();
    }

    @Override
    public WalletResponseDTO addMoney(String userId, AddMoneyRequestDTO addMoneyRequest) {
        log.info("Adding money to wallet for userId: {} amount: {}", userId, addMoneyRequest.getAmount());

        validateTransactionAmount(addMoneyRequest.getAmount());

        // Fix #4: was findtyUserId (typo)
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));

        validateWalletStatus(wallet);

        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .walletId(wallet.getWalletId())
                .userId(userId)
                .amount(addMoneyRequest.getAmount())
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.PENDING)
                .description(addMoneyRequest.getDescription() != null
                        ? addMoneyRequest.getDescription() : "Money added to wallet")
                .paymentMethod(addMoneyRequest.getPaymentMethod())
                .referenceId(addMoneyRequest.getReferenceId())
                .createdAt(LocalDateTime.now())
                .build();

        // Fix #5: try block should wrap the balance update + save logic
        try {
            BigDecimal newBalance = wallet.getBalance().add(addMoneyRequest.getAmount());
            wallet.setBalance(newBalance);
            wallet.setUpdatedAt(LocalDateTime.now());

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCompletedAt(LocalDateTime.now());
            transaction.setBalanceAfter(newBalance);

            walletRepository.save(wallet);
            transactionRepository.save(transaction);

            kafkaProducerService.publishWalletEvent(
                    WalletEvent.builder()
                            .eventType("MONEY_ADDED")
                            .userId(userId)
                            .walletId(wallet.getWalletId())
                            .transactionId(transaction.getTransactionId())
                            .amount(addMoneyRequest.getAmount())
                            .balanceAfter(newBalance)
                            .currency("INR")
                            .occurredAt(LocalDateTime.now())
                            .build()
            );

            log.info("Money added successfully. New balance: {} for userId: {}", newBalance, userId);
            return mapToWalletResponseDTO(wallet);

        } catch (Exception e) {
            // Fix #6: was TransactionStatus, FAILED (comma instead of dot)
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            kafkaProducerService.publishWalletEvent(
                    WalletEvent.builder()
                            .eventType("TRANSACTION_FAILED")
                            .userId(userId)
                            .reason(e.getMessage())
                            .occurredAt(LocalDateTime.now())
                            .build()
            );
            log.error("Failed to add money for userId: {}", userId, e);
            throw new TransactionFailedException("Failed to add money: " + e.getMessage());
        }
    }

    @Override
    public WalletResponseDTO withdrawMoney(String userId, WithdrawMoneyRequestDTO withdrawRequest) {
        log.info("Withdrawing money from wallet for userId: {} amount: {}", userId, withdrawRequest.getAmount());

        validateTransactionAmount(withdrawRequest.getAmount());

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));

        validateWalletStatus(wallet);

        if (wallet.getBalance().compareTo(withdrawRequest.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + wallet.getBalance() +
                            ", Required: " + withdrawRequest.getAmount());
        }

        Transaction transaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .walletId(wallet.getWalletId())
                .userId(userId)
                .amount(withdrawRequest.getAmount())
                .type(TransactionType.DEBIT)
                .status(TransactionStatus.PENDING)
                .description(withdrawRequest.getDescription() != null
                        ? withdrawRequest.getDescription() : "Money withdrawn from wallet")
                .bankAccountNumber(withdrawRequest.getBankAccountNumber())
                .ifscCode(withdrawRequest.getIfscCode())
                .createdAt(LocalDateTime.now())
                .build();

        try {
            BigDecimal newBalance = wallet.getBalance().subtract(withdrawRequest.getAmount());
            wallet.setBalance(newBalance);
            wallet.setUpdatedAt(LocalDateTime.now());
            wallet.setDailySpent(wallet.getDailySpent().add(withdrawRequest.getAmount()));
            wallet.setMonthlySpent(wallet.getMonthlySpent().add(withdrawRequest.getAmount()));

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCompletedAt(LocalDateTime.now());
            transaction.setBalanceAfter(newBalance);

            walletRepository.save(wallet);
            transactionRepository.save(transaction);

            kafkaProducerService.publishWalletEvent(
                    WalletEvent.builder()
                            .eventType("MONEY_WITHDRAWN")
                            .userId(userId)
                            .walletId(wallet.getWalletId())
                            .transactionId(transaction.getTransactionId())
                            .amount(withdrawRequest.getAmount())
                            .balanceAfter(newBalance)
                            .currency("INR")
                            .occurredAt(LocalDateTime.now())
                            .build()
            );

            if(newBalance.compareTo(new BigDecimal("500")) < 0){

                kafkaProducerService.publishWalletEvent(
                        WalletEvent.builder()
                                .eventType("LOW_BALANCE_ALERT")
                                .userId(userId)
                                .walletId(wallet.getWalletId())
                                .balanceAfter(newBalance)
                                .currency("INR")
                                .occurredAt(LocalDateTime.now())
                                .build()
                );
            }

            log.info("Money withdrawn successfully. New balance: {} for userId: {}", newBalance, userId);
            return mapToWalletResponseDTO(wallet);

        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            kafkaProducerService.publishWalletEvent(
                    WalletEvent.builder()
                            .eventType("TRANSACTION_FAILED")
                            .userId(userId)          // (fromUserId in the transfer one)
                            .reason(e.getMessage())
                            .occurredAt(LocalDateTime.now())
                            .build()
            );
            log.error("Failed to withdraw money for userId: {}", userId, e);
            throw new TransactionFailedException("Failed to withdraw money: " + e.getMessage());
        }
    }

    // ==================== TRANSFER OPERATIONS ====================

    @Override
    public TransferResponseDTO transferMoney(String fromUserId, TransferMoneyRequestDTO transferRequest) {
        log.info("Transferring money from userId: {} to userId: {} amount: {}",
                fromUserId, transferRequest.getToUserId(), transferRequest.getAmount());

        validateTransactionAmount(transferRequest.getAmount());

        Wallet fromWallet = walletRepository.findByUserId(fromUserId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + fromUserId));

        Wallet toWallet = walletRepository.findByUserId(transferRequest.getToUserId())
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for recipient user: " + transferRequest.getToUserId()));

        validateWalletStatus(fromWallet);
        validateWalletStatus(toWallet);

        if (fromUserId.equals(transferRequest.getToUserId())) {
            throw new InvalidTransferException("Cannot transfer money to your own wallet");
        }

        if (fromWallet.getBalance().compareTo(transferRequest.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + fromWallet.getBalance() +
                            ", Required: " + transferRequest.getAmount());
        }

        checkDailyLimit(fromWallet, transferRequest.getAmount());

        String transferId = UUID.randomUUID().toString();

        Transaction debitTransaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .walletId(fromWallet.getWalletId())
                .userId(fromUserId)
                .amount(transferRequest.getAmount())
                .type(TransactionType.TRANSFER_DEBIT)
                .status(TransactionStatus.PENDING)
                .description("Money transferred to " + transferRequest.getToUserId())
                .transferId(transferId)
                .recipientUserId(transferRequest.getToUserId())
                .createdAt(LocalDateTime.now())
                .build();

        Transaction creditTransaction = Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .walletId(toWallet.getWalletId())
                .userId(transferRequest.getToUserId())
                .amount(transferRequest.getAmount())
                .type(TransactionType.TRANSFER_CREDIT)
                .status(TransactionStatus.PENDING)
                .description("Money received from " + fromUserId)
                .transferId(transferId)
                .senderUserId(fromUserId)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            BigDecimal newFromBalance = fromWallet.getBalance().subtract(transferRequest.getAmount());
            fromWallet.setBalance(newFromBalance);
            fromWallet.setDailySpent(fromWallet.getDailySpent().add(transferRequest.getAmount()));
            fromWallet.setMonthlySpent(fromWallet.getMonthlySpent().add(transferRequest.getAmount()));
            fromWallet.setUpdatedAt(LocalDateTime.now());

            BigDecimal newToBalance = toWallet.getBalance().add(transferRequest.getAmount());
            toWallet.setBalance(newToBalance);
            toWallet.setUpdatedAt(LocalDateTime.now());

            debitTransaction.setStatus(TransactionStatus.SUCCESS);
            debitTransaction.setCompletedAt(LocalDateTime.now());
            debitTransaction.setBalanceAfter(newFromBalance);

            // Fix #7: was TransactionStatus SUCCESS (missing dot)
            creditTransaction.setStatus(TransactionStatus.SUCCESS);
            creditTransaction.setCompletedAt(LocalDateTime.now());
            creditTransaction.setBalanceAfter(newToBalance);

            walletRepository.save(fromWallet);
            walletRepository.save(toWallet);
            transactionRepository.save(debitTransaction);
            transactionRepository.save(creditTransaction);

            kafkaProducerService.publishWalletEvent(
                    WalletEvent.builder()
                            .eventType("MONEY_TRANSFERRED_DEBIT")
                            .userId(fromUserId)
                            .recipientUserId(transferRequest.getToUserId())
                            .recipientName(transferRequest.getRecipientName())
                            .amount(transferRequest.getAmount())
                            .balanceAfter(newFromBalance)
                            .currency("INR")
                            .transferId(transferId)
                            .transactionId(debitTransaction.getTransactionId())
                            .occurredAt(LocalDateTime.now())
                            .build()
            );

            kafkaProducerService.publishWalletEvent(
                    WalletEvent.builder()
                            .eventType("MONEY_TRANSFERRED_CREDIT")
                            .userId(transferRequest.getToUserId())
                            .senderUserId(fromUserId)           // <-- fixed: was being stuffed into recipientUserId
                            .senderName(transferRequest.getSenderName())
                            .amount(transferRequest.getAmount())
                            .balanceAfter(newToBalance)
                            .currency("INR")
                            .transferId(transferId)
                            .transactionId(creditTransaction.getTransactionId())
                            .occurredAt(LocalDateTime.now())
                            .build()
            );

            log.info("Money transferred successfully. TransferId: {}", transferId);

            return TransferResponseDTO.builder()
                    .transferId(transferId)
                    .fromUserId(fromUserId)
                    .toUserId(transferRequest.getToUserId())
                    .amount(transferRequest.getAmount())
                    .status(TransactionStatus.SUCCESS.toString())
                    .senderBalanceAfter(newFromBalance)
                    .transferredAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            debitTransaction.setStatus(TransactionStatus.FAILED);
            debitTransaction.setFailureReason(e.getMessage());
            creditTransaction.setStatus(TransactionStatus.FAILED);
            creditTransaction.setFailureReason(e.getMessage());
            transactionRepository.save(debitTransaction);
            transactionRepository.save(creditTransaction);
            kafkaProducerService.publishWalletEvent(
                    WalletEvent.builder()
                            .eventType("TRANSACTION_FAILED")
                            .userId(fromUserId)          // (fromUserId in the transfer one)
                            .reason(e.getMessage())
                            .occurredAt(LocalDateTime.now())
                            .build()
            );
            log.error("Failed to transfer money from userId: {} to userId: {}",
                    fromUserId, transferRequest.getToUserId(), e);
            throw new TransactionFailedException("Failed to transfer money: " + e.getMessage());
        }
    }

    // ==================== TRANSACTION HISTORY ====================

    @Override
    public Page<TransactionResponseDTO> getTransactionHistory(String userId, Pageable pageable) {
        log.info("Fetching transaction history for userId: {}", userId);
        walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        Page<Transaction> transactions = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return transactions.map(this::mapToTransactionResponseDTO);
    }

    @Override
    public TransactionResponseDTO getTransactionById(String userId, String transactionId) {
        log.info("Fetching transaction {} for userId: {}", transactionId, userId);
        Transaction transaction = transactionRepository.findByTransactionIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id: " + transactionId));
        return mapToTransactionResponseDTO(transaction);
    }

    @Override
    public List<TransactionResponseDTO> getTransactionsByDateRange(
            String userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching transactions for userId: {} between {} and {}", userId, startDate, endDate);
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndCreatedAtBetween(userId, startDate, endDate);
        return transactions.stream()
                .map(this::mapToTransactionResponseDTO)
                .toList();
    }

    // ==================== WALLET STATUS MANAGEMENT ====================

    @Override
    public void activateWallet(String userId) {
        log.info("Activating wallet for userId: {}", userId);
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
        log.info("Wallet activated successfully for userId: {}", userId);
    }

    @Override
    public void freezeWallet(String userId, String reason) {
        log.info("Freezing wallet for userId: {} with reason: {}", userId, reason);
        // Fix #8: was arElseThrow (typo)
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        wallet.setStatus(WalletStatus.FROZEN);
        wallet.setFreezeReason(reason);
        wallet.setFrozenAt(LocalDateTime.now());
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
        log.info("Wallet frozen successfully for userId: {}", userId);
    }

    @Override
    public void suspendWallet(String userId, String reason) {
        log.info("Suspending wallet for userId: {} with reason: {}", userId, reason);
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        wallet.setStatus(WalletStatus.SUSPENDED);
        wallet.setSuspensionReason(reason);
        wallet.setSuspendedAt(LocalDateTime.now());
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
        log.info("Wallet suspended successfully for userId: {}", userId);
    }

    @Override
    public WalletStatus getWalletStatus(String userId) {
        log.info("Fetching wallet status for userId: {}", userId);
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        return wallet.getStatus();
    }

    // ==================== LIMITS MANAGEMENT ====================

    @Override
    public void updateDailyLimit(String userId, BigDecimal newLimit) {
        log.info("Updating daily limit for userId: {} to {}", userId, newLimit);
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        wallet.setDailyLimit(newLimit);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
        log.info("Daily limit updated successfully for userId: {}", userId);
    }

    @Override
    public void updateMonthlyLimit(String userId, BigDecimal newLimit) {
        log.info("Updating monthly limit for userId: {} to {}", userId, newLimit);
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        wallet.setMonthlyLimit(newLimit);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
        log.info("Monthly limit updated successfully for userId: {}", userId);
    }

    @Override
    public void resetDailySpent(String userId) {
        log.info("Resetting daily spent for userId: {}", userId);
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user: " + userId));
        wallet.setDailySpent(BigDecimal.ZERO);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
        log.info("Daily spent reset successfully for userId: {}", userId);
    }

    // ==================== VALIDATION METHODS ====================

    private void validateTransactionAmount(BigDecimal amount) {
        if (amount.compareTo(MIN_BALANCE) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new InvalidAmountException(
                    "Amount exceeds maximum transaction limit of " + MAX_TRANSACTION_AMOUNT);
        }
    }

    private void validateWalletStatus(Wallet wallet) {
        if (wallet.getStatus() == WalletStatus.FROZEN) {
            throw new WalletFrozenException("Wallet is frozen: " + wallet.getFreezeReason());
        }
        if (wallet.getStatus() == WalletStatus.SUSPENDED) {
            throw new WalletSuspendedException("Wallet is suspended: " + wallet.getSuspensionReason());
        }
        if (wallet.getStatus() == WalletStatus.CLOSED) {
            throw new WalletClosedException("Wallet is closed and cannot be used");
        }
    }

    private void checkDailyLimit(Wallet wallet, BigDecimal amount) {
        BigDecimal totalSpent = wallet.getDailySpent().add(amount);
        if (totalSpent.compareTo(wallet.getDailyLimit()) > 0) {
            throw new DailyLimitExceededException(
                    "Daily limit exceeded. Limit: " + wallet.getDailyLimit() +
                            ", Already spent: " + wallet.getDailySpent() +
                            ", Requested: " + amount);
        }
    }

    // ==================== MAPPER METHODS ====================

    private WalletResponseDTO mapToWalletResponseDTO(Wallet wallet) {
        return WalletResponseDTO.builder()
                .walletId(wallet.getWalletId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .status(wallet.getStatus().toString())
                .dailyLimit(wallet.getDailyLimit())
                .monthlyLimit(wallet.getMonthlyLimit())
                .dailySpent(wallet.getDailySpent())
                .monthlySpent(wallet.getMonthlySpent())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    private TransactionResponseDTO mapToTransactionResponseDTO(Transaction transaction) {
        return TransactionResponseDTO.builder()
                .transactionId(transaction.getTransactionId())
                .walletId(transaction.getWalletId())
                .userId(transaction.getUserId())
                .amount(transaction.getAmount())
                .type(transaction.getType().toString())
                .status(transaction.getStatus().toString())
                .description(transaction.getDescription())
                .balanceAfter(transaction.getBalanceAfter())
                .transferId(transaction.getTransferId())
                .senderUserId(transaction.getSenderUserId())
                .recipientUserId(transaction.getRecipientUserId())
                .paymentMethod(transaction.getPaymentMethod())
                .referenceId(transaction.getReferenceId())
                .failureReason(transaction.getFailureReason())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }
}
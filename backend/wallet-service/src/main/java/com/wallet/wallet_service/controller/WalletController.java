package com.wallet.wallet_service.controller;

import com.wallet.wallet_service.dto.request.*;
import com.wallet.wallet_service.dto.response.*;
import com.wallet.wallet_service.enums.WalletStatus;
import com.wallet.wallet_service.exception.UnauthorizedAccessException;
import com.wallet.wallet_service.security.UserIdResolver;
import com.wallet.wallet_service.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wallet Management", description = "APIs for wallet operations, transactions, and balance management")
public class WalletController {

    private final WalletService walletService;
    private final UserIdResolver userIdResolver;

    // ==================== WALLET CREATION & MANAGEMENT ====================

    @PostMapping("/create")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create wallet", description = "Create a new wallet for the authenticated user")
    public ResponseEntity<ApiResponse<WalletResponseDTO>> createWallet(
            HttpServletRequest request,
            @RequestHeader("X-User-Id") String userId) {

        String verifiedUserId = userIdResolver.resolve(request, userId);
        log.info("POST /api/wallet/create - Creating wallet for userId: {}", verifiedUserId);

        WalletResponseDTO walletResponse = walletService.createWallet(verifiedUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<WalletResponseDTO>builder()
                        .success(true)
                        .message("Wallet created successfully")
                        .data(walletResponse)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @GetMapping("/user/{userId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get wallet by userId", description = "Fetch wallet details by user ID")
    public ResponseEntity<ApiResponse<WalletResponseDTO>> getWalletByUserId(
            HttpServletRequest request,
            @PathVariable String userId,
            @RequestHeader("X-User-Id") String requestingUserId) {

        String verifiedUserId = userIdResolver.resolve(request, requestingUserId);

        if (!userId.equals(verifiedUserId)) {
            log.warn("User {} attempted to access wallet of user {}", verifiedUserId, userId);
            throw new UnauthorizedAccessException("You can only view your own wallet");
        }

        log.info("GET /api/wallet/user/{} - Fetching wallet", userId);
        WalletResponseDTO walletResponse = walletService.getWalletByUserId(userId);

        return ResponseEntity.ok(
                ApiResponse.<WalletResponseDTO>builder()
                        .success(true)
                        .message("Wallet fetched successfully")
                        .data(walletResponse)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @GetMapping("/{walletId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get wallet by walletId", description = "Fetch wallet details by wallet ID")
    public ResponseEntity<ApiResponse<WalletResponseDTO>> getWalletById(
            HttpServletRequest request,
            @PathVariable String walletId,
            @RequestHeader("X-User-Id") String requestingUserId) {

        String verifiedUserId = userIdResolver.resolve(request, requestingUserId);
        log.info("GET /api/wallet/{} - Fetching wallet", walletId);

        WalletResponseDTO walletResponse = walletService.getWalletById(walletId);

        if (!walletResponse.getUserId().equals(verifiedUserId)) {
            log.warn("User {} attempted to access wallet {}", verifiedUserId, walletId);
            throw new UnauthorizedAccessException("You can only view your own wallet");
        }

        return ResponseEntity.ok(
                ApiResponse.<WalletResponseDTO>builder()
                        .success(true)
                        .message("Wallet fetched successfully")
                        .data(walletResponse)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== BALANCE OPERATIONS ====================

    @GetMapping("/balance")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get wallet balance", description = "Fetch current wallet balance")
    public ResponseEntity<ApiResponse<BalanceResponseDTO>> getBalance(
            HttpServletRequest request,
            @RequestHeader("X-User-Id") String userId) {

        String verifiedUserId = userIdResolver.resolve(request, userId);
        log.info("GET /api/wallet/balance - Fetching balance for userId: {}", verifiedUserId);

        BalanceResponseDTO balanceResponse = walletService.getBalance(verifiedUserId);

        return ResponseEntity.ok(
                ApiResponse.<BalanceResponseDTO>builder()
                        .success(true)
                        .message("Balance fetched successfully")
                        .data(balanceResponse)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PostMapping("/add-money")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add money to wallet", description = "Add money to wallet from external source")
    public ResponseEntity<ApiResponse<WalletResponseDTO>> addMoney(
            HttpServletRequest request,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AddMoneyRequestDTO addMoneyRequest) {

        String verifiedUserId = userIdResolver.resolve(request, userId);
        log.info("POST /api/wallet/add-money - Adding money for userId: {} amount: {}",
                verifiedUserId, addMoneyRequest.getAmount());

        WalletResponseDTO walletResponse = walletService.addMoney(verifiedUserId, addMoneyRequest);

        return ResponseEntity.ok(
                ApiResponse.<WalletResponseDTO>builder()
                        .success(true)
                        .message("Money added successfully")
                        .data(walletResponse)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PostMapping("/withdraw-money")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Withdraw money from wallet", description = "Withdraw money to bank account")
    public ResponseEntity<ApiResponse<WalletResponseDTO>> withdrawMoney(
            HttpServletRequest request,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody WithdrawMoneyRequestDTO withdrawRequest) {

        String verifiedUserId = userIdResolver.resolve(request, userId);
        log.info("POST /api/wallet/withdraw-money - Withdrawing money for userId: {} amount: {}",
                verifiedUserId, withdrawRequest.getAmount());

        WalletResponseDTO walletResponse = walletService.withdrawMoney(verifiedUserId, withdrawRequest);

        return ResponseEntity.ok(
                ApiResponse.<WalletResponseDTO>builder()
                        .success(true)
                        .message("Money withdrawn successfully")
                        .data(walletResponse)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== TRANSFER OPERATIONS ====================

    @PostMapping("/transfer")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Transfer money", description = "Transfer money to another wallet")
    public ResponseEntity<ApiResponse<TransferResponseDTO>> transferMoney(
            HttpServletRequest request,
            @RequestHeader("X-User-Id") String fromUserId,
            @Valid @RequestBody TransferMoneyRequestDTO transferRequest) {

        String verifiedUserId = userIdResolver.resolve(request, fromUserId);
        log.info("POST /api/wallet/transfer - Transferring money from userId: {} to userId: {} amount: {}",
                verifiedUserId, transferRequest.getToUserId(), transferRequest.getAmount());

        TransferResponseDTO transferResponse = walletService.transferMoney(verifiedUserId, transferRequest);

        return ResponseEntity.ok(
                ApiResponse.<TransferResponseDTO>builder()
                        .success(true)
                        .message("Money transferred successfully")
                        .data(transferResponse)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== TRANSACTION HISTORY ====================

    @GetMapping("/transactions")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get transaction history", description = "Fetch paginated transaction history")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDTO>>> getTransactionHistory(
            HttpServletRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String verifiedUserId = userIdResolver.resolve(request, userId);
        log.info("GET /api/wallet/transactions - Fetching transactions for userId: {} page: {} size: {}",
                verifiedUserId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionResponseDTO> transactions =
                walletService.getTransactionHistory(verifiedUserId, pageable);

        return ResponseEntity.ok(
                ApiResponse.<Page<TransactionResponseDTO>>builder()
                        .success(true)
                        .message("Transactions fetched successfully")
                        .data(transactions)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @GetMapping("/transactions/date-range")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get transactions by date range", description = "Fetch transactions within date range")
    public ResponseEntity<ApiResponse<List<TransactionResponseDTO>>> getTransactionsByDateRange(
            HttpServletRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        String verifiedUserId = userIdResolver.resolve(request, userId);
        log.info("GET /api/wallet/transactions/date-range - userId: {} from {} to {}",
                verifiedUserId, startDate, endDate);

        List<TransactionResponseDTO> transactions =
                walletService.getTransactionsByDateRange(verifiedUserId, startDate, endDate);

        return ResponseEntity.ok(
                ApiResponse.<List<TransactionResponseDTO>>builder()
                        .success(true)
                        .message("Transactions fetched successfully")
                        .data(transactions)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @GetMapping("/transactions/{transactionId}")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get transaction by ID", description = "Fetch specific transaction details")
    public ResponseEntity<ApiResponse<TransactionResponseDTO>> getTransactionById(
            HttpServletRequest request,
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String transactionId) {

        String verifiedUserId = userIdResolver.resolve(request, userId);
        log.info("GET /api/wallet/transactions/{} - userId: {}", transactionId, verifiedUserId);

        TransactionResponseDTO transaction =
                walletService.getTransactionById(verifiedUserId, transactionId);

        return ResponseEntity.ok(
                ApiResponse.<TransactionResponseDTO>builder()
                        .success(true)
                        .message("Transaction fetched successfully")
                        .data(transaction)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== WALLET STATUS MANAGEMENT ====================

    @GetMapping("/status")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get wallet status", description = "Fetch wallet status")
    public ResponseEntity<ApiResponse<WalletStatusResponseDTO>> getWalletStatus(
            HttpServletRequest request,
            @RequestHeader("X-User-Id") String userId) {

        String verifiedUserId = userIdResolver.resolve(request, userId);
        log.info("GET /api/wallet/status - userId: {}", verifiedUserId);

        WalletStatus status = walletService.getWalletStatus(verifiedUserId);

        WalletStatusResponseDTO statusResponse = WalletStatusResponseDTO.builder()
                .userId(verifiedUserId)
                .status(status.toString())
                .build();

        return ResponseEntity.ok(
                ApiResponse.<WalletStatusResponseDTO>builder()
                        .success(true)
                        .message("Wallet status fetched successfully")
                        .data(statusResponse)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PatchMapping("/activate")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Activate wallet", description = "Activate user wallet")
    public ResponseEntity<ApiResponse<Void>> activateWallet(
            HttpServletRequest request,
            @RequestHeader("X-User-Id") String userId) {

        String verifiedUserId = userIdResolver.resolve(request, userId);
        log.info("PATCH /api/wallet/activate - userId: {}", verifiedUserId);

        walletService.activateWallet(verifiedUserId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Wallet activated successfully")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PatchMapping("/freeze")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Freeze wallet", description = "Freeze wallet (Admin only)")
    public ResponseEntity<ApiResponse<Void>> freezeWallet(
            @RequestParam String userId,
            @Valid @RequestBody FreezeWalletRequestDTO freezeRequest) {

        // Admin-only — no UserIdResolver needed, ROLE_ADMIN check is the guard
        log.info("PATCH /api/wallet/freeze - Freezing wallet for userId: {}", userId);

        walletService.freezeWallet(userId, freezeRequest.getReason());

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Wallet frozen successfully")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PatchMapping("/suspend")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend wallet", description = "Suspend wallet (Admin only)")
    public ResponseEntity<ApiResponse<Void>> suspendWallet(
            @RequestParam String userId,
            @Valid @RequestBody SuspendWalletRequestDTO suspendRequest) {

        log.info("PATCH /api/wallet/suspend - Suspending wallet for userId: {}", userId);

        walletService.suspendWallet(userId, suspendRequest.getReason());

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Wallet suspended successfully")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== LIMITS MANAGEMENT ====================

    @PatchMapping("/limits/daily")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update daily limit", description = "Update wallet daily transaction limit (Admin only)")
    public ResponseEntity<ApiResponse<Void>> updateDailyLimit(
            @RequestParam String userId,
            @Valid @RequestBody UpdateLimitRequestDTO limitRequest) {

        log.info("PATCH /api/wallet/limits/daily - userId: {} limit: {}", userId, limitRequest.getLimit());

        walletService.updateDailyLimit(userId, limitRequest.getLimit());

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Daily limit updated successfully")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PatchMapping("/limits/monthly")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update monthly limit", description = "Update wallet monthly transaction limit (Admin only)")
    public ResponseEntity<ApiResponse<Void>> updateMonthlyLimit(
            @RequestParam String userId,
            @Valid @RequestBody UpdateLimitRequestDTO limitRequest) {

        log.info("PATCH /api/wallet/limits/monthly - userId: {} limit: {}", userId, limitRequest.getLimit());

        walletService.updateMonthlyLimit(userId, limitRequest.getLimit());

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Monthly limit updated successfully")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PostMapping("/limits/reset-daily")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reset daily spent", description = "Reset daily spent amount (Admin only)")
    public ResponseEntity<ApiResponse<Void>> resetDailySpent(@RequestParam String userId) {

        log.info("POST /api/wallet/limits/reset-daily - userId: {}", userId);

        walletService.resetDailySpent(userId);

        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Daily spent reset successfully")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== HEALTH CHECK ====================

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if wallet service is running")
    public ResponseEntity<ApiResponse<String>> healthCheck() {

        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Wallet service is running")
                        .data("OK")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }
}
package com.wallet.notification_service.service;

import com.wallet.notification_service.dto.NotificationLogDTO;
import com.wallet.notification_service.dto.UserPreferenceDTO;
import com.wallet.notification_service.entity.NotificationLog;
import com.wallet.notification_service.entity.UserPreference;
import com.wallet.notification_service.enums.NotificationStatus;
import com.wallet.notification_service.enums.NotificationType;
import com.wallet.notification_service.event.WalletEvent;
import com.wallet.notification_service.exception.NotificationNotFoundException;
import com.wallet.notification_service.exception.UserPreferenceNotFoundException;
import com.wallet.notification_service.repository.NotificationLogRepository;
import com.wallet.notification_service.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final EmailService emailService;

    private static final int MAX_RETRY = 3;

    // ==================== MAIN EVENT DISPATCHER ====================

    @Override
    public void processWalletEvent(WalletEvent event) {
        log.info("Processing wallet event: type={} userId={}", event.getEventType(), event.getUserId());

        UserPreference pref = userPreferenceRepository.findByUserId(event.getUserId())
                .orElse(null);

        if (pref == null) {
            log.warn("No preferences found for userId: {} — skipping notification", event.getUserId());
            persistLog(event, NotificationStatus.SKIPPED, null, "No user preference found", null, null);
            return;
        }

        switch (event.getEventType()) {
            case "MONEY_ADDED"               -> handleMoneyAdded(event, pref);
            case "MONEY_WITHDRAWN"           -> handleMoneyWithdrawn(event, pref);
            case "MONEY_TRANSFERRED_DEBIT"   -> handleTransferDebit(event, pref);
            case "MONEY_TRANSFERRED_CREDIT"  -> handleTransferCredit(event, pref);
            case "WALLET_FROZEN"             -> handleWalletFrozen(event, pref);
            case "WALLET_SUSPENDED"          -> handleWalletSuspended(event, pref);
            case "DAILY_LIMIT_ALERT"         -> handleDailyLimitAlert(event, pref);
            case "LOW_BALANCE_ALERT"         -> handleLowBalanceAlert(event, pref);
            case "TRANSACTION_FAILED"        -> handleTransactionFailed(event, pref);
            default -> log.warn("Unknown event type: {} — no handler registered", event.getEventType());
        }
    }

    // ==================== EVENT HANDLERS ====================

    private void handleMoneyAdded(WalletEvent event, UserPreference pref) {
        if (!pref.isCreditAlerts() || !pref.isEmailEnabled()) {
            persistLog(event, NotificationStatus.SKIPPED, pref.getEmail(), "User opted out of credit alerts", null, null);
            return;
        }
        sendEmailWithLog(event, pref.getEmail(),
                "✅ Money Added to Your VaultX Wallet",
                () -> emailService.sendMoneyAddedEmail(pref.getEmail(), event));
    }

    private void handleMoneyWithdrawn(WalletEvent event, UserPreference pref) {
        if (!pref.isDebitAlerts() || !pref.isEmailEnabled()) {
            persistLog(event, NotificationStatus.SKIPPED, pref.getEmail(), "User opted out of debit alerts", null, null);
            return;
        }
        sendEmailWithLog(event, pref.getEmail(),
                "💸 Withdrawal Successful – VaultX Wallet",
                () -> emailService.sendMoneyWithdrawnEmail(pref.getEmail(), event));
    }

    private void handleTransferDebit(WalletEvent event, UserPreference pref) {
        if (!pref.isTransferAlerts() || !pref.isEmailEnabled()) {
            persistLog(event, NotificationStatus.SKIPPED, pref.getEmail(), "User opted out of transfer alerts", null, null);
            return;
        }
        sendEmailWithLog(event, pref.getEmail(),
                "🔄 Money Transferred – VaultX Wallet",
                () -> emailService.sendTransferDebitEmail(pref.getEmail(), event));
    }

    private void handleTransferCredit(WalletEvent event, UserPreference pref) {
        if (!pref.isTransferAlerts() || !pref.isEmailEnabled()) {
            persistLog(event, NotificationStatus.SKIPPED, pref.getEmail(), "User opted out of transfer alerts", null, null);
            return;
        }
        sendEmailWithLog(event, pref.getEmail(),
                "🎉 Money Received – VaultX Wallet",
                () -> emailService.sendTransferCreditEmail(pref.getEmail(), event));
    }

    private void handleWalletFrozen(WalletEvent event, UserPreference pref) {
        if (!pref.isSecurityAlerts() || !pref.isEmailEnabled()) {
            persistLog(event, NotificationStatus.SKIPPED, pref.getEmail(), "User opted out of security alerts", null, null);
            return;
        }
        // Security alerts ignore opt-out for critical events — always send if email available
        sendEmailWithLog(event, pref.getEmail(),
                "🔒 Your VaultX Wallet Has Been Frozen",
                () -> emailService.sendWalletFrozenEmail(pref.getEmail(), event));
    }

    private void handleWalletSuspended(WalletEvent event, UserPreference pref) {
        sendEmailWithLog(event, pref.getEmail(),
                "⚠️ Your VaultX Wallet Has Been Suspended",
                () -> emailService.sendWalletSuspendedEmail(pref.getEmail(), event));
    }

    private void handleDailyLimitAlert(WalletEvent event, UserPreference pref) {
        if (!pref.isSpendAlerts() || !pref.isEmailEnabled()) {
            persistLog(event, NotificationStatus.SKIPPED, pref.getEmail(), "User opted out of spend alerts", null, null);
            return;
        }
        sendEmailWithLog(event, pref.getEmail(),
                "🚨 Daily Spend Limit Alert",
                () -> emailService.sendDailyLimitAlertEmail(pref.getEmail(), event));
    }

    private void handleLowBalanceAlert(WalletEvent event, UserPreference pref) {
        if (!pref.isSpendAlerts() || !pref.isEmailEnabled()) {
            persistLog(event, NotificationStatus.SKIPPED, pref.getEmail(), "User opted out of spend alerts", null, null);
            return;
        }
        sendEmailWithLog(event, pref.getEmail(),
                "⚡ Low Balance Alert",
                () -> emailService.sendLowBalanceAlertEmail(pref.getEmail(), event));
    }

    private void handleTransactionFailed(WalletEvent event, UserPreference pref) {
        if (!pref.isEmailEnabled()) {
            persistLog(event, NotificationStatus.SKIPPED, pref.getEmail(), "Email disabled", null, null);
            return;
        }
        sendEmailWithLog(event, pref.getEmail(),
                "❌ Transaction Failed",
                () -> emailService.sendTransactionFailedEmail(pref.getEmail(), event));
    }

    // ==================== SEND + LOG HELPER ====================

    /**
     * Calls the emailSender lambda, persists SUCCESS or FAILED log.
     * This is the single place where we handle try/catch for all email sends.
     */
    private void sendEmailWithLog(WalletEvent event, String email, String subject, Runnable emailSender) {
        NotificationLog log = buildPendingLog(event, email, subject);
        notificationLogRepository.save(log);

        try {
            emailSender.run();
            log.setStatus(NotificationStatus.SENT);
            log.setSentAt(LocalDateTime.now());
            log.setUpdatedAt(LocalDateTime.now());
            notificationLogRepository.save(log);
            this.log.info("Notification sent | notificationId={} userId={} type={}",
                    log.getNotificationId(), event.getUserId(), event.getEventType());

        } catch (Exception e) {
            log.setStatus(NotificationStatus.FAILED);
            log.setFailureReason(e.getMessage());
            log.setUpdatedAt(LocalDateTime.now());
            notificationLogRepository.save(log);
            this.log.error("Notification failed | notificationId={} userId={} error={}",
                    log.getNotificationId(), event.getUserId(), e.getMessage());
        }
    }

    private NotificationLog buildPendingLog(WalletEvent event, String email, String subject) {
        return NotificationLog.builder()
                .notificationId(UUID.randomUUID().toString())
                .userId(event.getUserId())
                .eventType(event.getEventType())
                .notificationType(NotificationType.EMAIL)
                .status(NotificationStatus.PENDING)
                .recipient(email)
                .subject(subject)
                .transactionId(event.getTransactionId())
                .walletId(event.getWalletId())
                .amount(event.getAmount())
                .balanceAfter(event.getBalanceAfter())
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void persistLog(WalletEvent event, NotificationStatus status,
                             String recipient, String failureReason,
                             String subject, String body) {
        NotificationLog logEntry = NotificationLog.builder()
                .notificationId(UUID.randomUUID().toString())
                .userId(event.getUserId())
                .eventType(event.getEventType())
                .notificationType(NotificationType.EMAIL)
                .status(status)
                .recipient(recipient)
                .subject(subject)
                .messageBody(body)
                .transactionId(event.getTransactionId())
                .walletId(event.getWalletId())
                .amount(event.getAmount())
                .balanceAfter(event.getBalanceAfter())
                .failureReason(failureReason)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        notificationLogRepository.save(logEntry);
    }

    // ==================== USER PREFERENCES ====================

    @Override
    public UserPreferenceDTO saveUserPreference(UserPreferenceDTO dto) {
        log.info("Saving user preference for userId: {}", dto.getUserId());

        if (userPreferenceRepository.existsByUserId(dto.getUserId())) {
            return updateUserPreference(dto.getUserId(), dto);
        }

        UserPreference pref = UserPreference.builder()
                .userId(dto.getUserId())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .emailEnabled(dto.isEmailEnabled())
                .smsEnabled(dto.isSmsEnabled())
                .creditAlerts(dto.isCreditAlerts())
                .debitAlerts(dto.isDebitAlerts())
                .transferAlerts(dto.isTransferAlerts())
                .securityAlerts(dto.isSecurityAlerts())
                .spendAlerts(dto.isSpendAlerts())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UserPreference saved = userPreferenceRepository.save(pref);
        log.info("User preference saved for userId: {}", saved.getUserId());
        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPreferenceDTO getUserPreference(String userId) {
        UserPreference pref = userPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new UserPreferenceNotFoundException("Preferences not found for userId: " + userId));
        return mapToDTO(pref);
    }

    @Override
    public UserPreferenceDTO updateUserPreference(String userId, UserPreferenceDTO dto) {
        log.info("Updating user preference for userId: {}", userId);

        UserPreference pref = userPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new UserPreferenceNotFoundException("Preferences not found for userId: " + userId));

        if (dto.getEmail() != null)       pref.setEmail(dto.getEmail());
        if (dto.getPhoneNumber() != null) pref.setPhoneNumber(dto.getPhoneNumber());

        pref.setEmailEnabled(dto.isEmailEnabled());
        pref.setSmsEnabled(dto.isSmsEnabled());
        pref.setCreditAlerts(dto.isCreditAlerts());
        pref.setDebitAlerts(dto.isDebitAlerts());
        pref.setTransferAlerts(dto.isTransferAlerts());
        pref.setSecurityAlerts(dto.isSecurityAlerts());
        pref.setSpendAlerts(dto.isSpendAlerts());
        pref.setUpdatedAt(LocalDateTime.now());

        return mapToDTO(userPreferenceRepository.save(pref));
    }

    // ==================== NOTIFICATION HISTORY ====================

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationLogDTO> getNotificationHistory(String userId, Pageable pageable) {
        return notificationLogRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToLogDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationLogDTO> getNotificationsByDateRange(
            String userId, LocalDateTime start, LocalDateTime end) {
        return notificationLogRepository
                .findByUserIdAndCreatedAtBetween(userId, start, end)
                .stream()
                .map(this::mapToLogDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationLogDTO getNotificationById(String notificationId) {
        NotificationLog nl = notificationLogRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(
                        "Notification not found: " + notificationId));
        return mapToLogDTO(nl);
    }

    // ==================== RETRY SCHEDULER ====================

    /**
     * Runs every 15 minutes — picks up FAILED notifications with retryCount < MAX_RETRY.
     * In production you'd use a DLQ (Dead Letter Queue) on Kafka instead,
     * but this covers basic retry without a DLQ setup.
     */
    @Override
    @Scheduled(fixedDelay = 900000) // 15 minutes
    public void retryFailedNotifications() {
        List<NotificationLog> failedLogs =
                notificationLogRepository.findByStatusAndRetryCountLessThan(
                        NotificationStatus.FAILED, MAX_RETRY);

        if (failedLogs.isEmpty()) return;

        log.info("Retrying {} failed notifications", failedLogs.size());

        for (NotificationLog nl : failedLogs) {
            try {
                emailService.sendHtmlEmail(nl.getRecipient(), nl.getSubject(), nl.getMessageBody());
                nl.setStatus(NotificationStatus.SENT);
                nl.setSentAt(LocalDateTime.now());
                log.info("Retry successful | notificationId={}", nl.getNotificationId());
            } catch (Exception e) {
                nl.setRetryCount(nl.getRetryCount() + 1);
                nl.setFailureReason(e.getMessage());
                log.error("Retry failed | notificationId={} retryCount={}", nl.getNotificationId(), nl.getRetryCount());
            }
            nl.setUpdatedAt(LocalDateTime.now());
            notificationLogRepository.save(nl);
        }
    }

    // ==================== MAPPERS ====================

    private UserPreferenceDTO mapToDTO(UserPreference pref) {
        return UserPreferenceDTO.builder()
                .userId(pref.getUserId())
                .email(pref.getEmail())
                .phoneNumber(pref.getPhoneNumber())
                .emailEnabled(pref.isEmailEnabled())
                .smsEnabled(pref.isSmsEnabled())
                .creditAlerts(pref.isCreditAlerts())
                .debitAlerts(pref.isDebitAlerts())
                .transferAlerts(pref.isTransferAlerts())
                .securityAlerts(pref.isSecurityAlerts())
                .spendAlerts(pref.isSpendAlerts())
                .createdAt(pref.getCreatedAt())
                .updatedAt(pref.getUpdatedAt())
                .build();
    }

    private NotificationLogDTO mapToLogDTO(NotificationLog nl) {
        return NotificationLogDTO.builder()
                .notificationId(nl.getNotificationId())
                .userId(nl.getUserId())
                .eventType(nl.getEventType())
                .notificationType(nl.getNotificationType().toString())
                .status(nl.getStatus().toString())
                .recipient(nl.getRecipient())
                .subject(nl.getSubject())
                .transactionId(nl.getTransactionId())
                .walletId(nl.getWalletId())
                .amount(nl.getAmount())
                .balanceAfter(nl.getBalanceAfter())
                .failureReason(nl.getFailureReason())
                .retryCount(nl.getRetryCount())
                .createdAt(nl.getCreatedAt())
                .sentAt(nl.getSentAt())
                .build();
    }
}

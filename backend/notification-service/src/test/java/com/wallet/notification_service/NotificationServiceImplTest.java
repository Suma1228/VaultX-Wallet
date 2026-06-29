package com.wallet.notification_service.service;

import com.wallet.notification_service.entity.NotificationLog;
import com.wallet.notification_service.entity.UserPreference;
import com.wallet.notification_service.enums.NotificationStatus;
import com.wallet.notification_service.event.WalletEvent;
import com.wallet.notification_service.exception.UserPreferenceNotFoundException;
import com.wallet.notification_service.repository.NotificationLogRepository;
import com.wallet.notification_service.repository.UserPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UserPreference activePreference;
    private WalletEvent moneyAddedEvent;

    @BeforeEach
    void setUp() {
        activePreference = UserPreference.builder()
                .userId("user-001")
                .email("swetha@vaultx.com")
                .emailEnabled(true)
                .creditAlerts(true)
                .debitAlerts(true)
                .transferAlerts(true)
                .securityAlerts(true)
                .spendAlerts(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        moneyAddedEvent = WalletEvent.builder()
                .eventId("evt-001")
                .eventType("MONEY_ADDED")
                .userId("user-001")
                .walletId("wallet-001")
                .transactionId("txn-001")
                .amount(new BigDecimal("500.00"))
                .balanceAfter(new BigDecimal("1500.00"))
                .currency("INR")
                .status("SUCCESS")
                .occurredAt(LocalDateTime.now())
                .build();
    }

    // ==================== processWalletEvent ====================

    @Test
    @DisplayName("Should send email and log SUCCESS when user has active preferences")
    void processWalletEvent_moneyAdded_sendsEmailAndLogsSuccess() {
        // Arrange
        when(userPreferenceRepository.findByUserId("user-001"))
                .thenReturn(Optional.of(activePreference));
        when(notificationLogRepository.save(any(NotificationLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendMoneyAddedEmail(anyString(), any(WalletEvent.class));

        // Act
        notificationService.processWalletEvent(moneyAddedEvent);

        // Assert
        verify(emailService, times(1))
                .sendMoneyAddedEmail("swetha@vaultx.com", moneyAddedEvent);

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, atLeastOnce()).save(logCaptor.capture());

        NotificationLog savedLog = logCaptor.getAllValues().stream()
                .filter(l -> l.getStatus() == NotificationStatus.SENT)
                .findFirst()
                .orElse(null);
        assertThat(savedLog).isNotNull();
        assertThat(savedLog.getUserId()).isEqualTo("user-001");
        assertThat(savedLog.getEventType()).isEqualTo("MONEY_ADDED");
    }

    @Test
    @DisplayName("Should skip notification and log SKIPPED when no user preference found")
    void processWalletEvent_noPreference_logsSkipped() {
        // Arrange
        when(userPreferenceRepository.findByUserId("user-001"))
                .thenReturn(Optional.empty());
        when(notificationLogRepository.save(any(NotificationLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        notificationService.processWalletEvent(moneyAddedEvent);

        // Assert
        verify(emailService, never()).sendMoneyAddedEmail(anyString(), any());

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.SKIPPED);
    }

    @Test
    @DisplayName("Should log SKIPPED when user has opted out of credit alerts")
    void processWalletEvent_creditAlertsDisabled_logsSkipped() {
        // Arrange
        activePreference.setCreditAlerts(false);
        when(userPreferenceRepository.findByUserId("user-001"))
                .thenReturn(Optional.of(activePreference));
        when(notificationLogRepository.save(any(NotificationLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        notificationService.processWalletEvent(moneyAddedEvent);

        // Assert
        verify(emailService, never()).sendMoneyAddedEmail(anyString(), any());
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SKIPPED);
    }

    @Test
    @DisplayName("Should log FAILED when email sending throws exception")
    void processWalletEvent_emailFails_logsFailure() {
        // Arrange
        when(userPreferenceRepository.findByUserId("user-001"))
                .thenReturn(Optional.of(activePreference));
        when(notificationLogRepository.save(any(NotificationLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP connection refused"))
                .when(emailService).sendMoneyAddedEmail(anyString(), any());

        // Act
        notificationService.processWalletEvent(moneyAddedEvent);

        // Assert
        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, atLeastOnce()).save(logCaptor.capture());

        NotificationLog failedLog = logCaptor.getAllValues().stream()
                .filter(l -> l.getStatus() == NotificationStatus.FAILED)
                .findFirst()
                .orElse(null);
        assertThat(failedLog).isNotNull();
        assertThat(failedLog.getFailureReason()).contains("SMTP connection refused");
    }

    @Test
    @DisplayName("Should handle WALLET_FROZEN event and call freeze email")
    void processWalletEvent_walletFrozen_sendsSecurityEmail() {
        // Arrange
        WalletEvent frozenEvent = WalletEvent.builder()
                .eventId("evt-002")
                .eventType("WALLET_FROZEN")
                .userId("user-001")
                .walletId("wallet-001")
                .reason("Suspicious activity detected")
                .occurredAt(LocalDateTime.now())
                .build();

        when(userPreferenceRepository.findByUserId("user-001"))
                .thenReturn(Optional.of(activePreference));
        when(notificationLogRepository.save(any(NotificationLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(emailService).sendWalletFrozenEmail(anyString(), any());

        // Act
        notificationService.processWalletEvent(frozenEvent);

        // Assert
        verify(emailService, times(1))
                .sendWalletFrozenEmail("swetha@vaultx.com", frozenEvent);
    }

    // ==================== getUserPreference ====================

    @Test
    @DisplayName("Should throw UserPreferenceNotFoundException when preference does not exist")
    void getUserPreference_notFound_throwsException() {
        when(userPreferenceRepository.findByUserId("unknown-user"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getUserPreference("unknown-user"))
                .isInstanceOf(UserPreferenceNotFoundException.class)
                .hasMessageContaining("unknown-user");
    }
}

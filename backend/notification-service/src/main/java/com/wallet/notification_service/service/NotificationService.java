package com.wallet.notification_service.service;

import com.wallet.notification_service.dto.NotificationLogDTO;
import com.wallet.notification_service.dto.UserPreferenceDTO;
import com.wallet.notification_service.event.WalletEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationService {

    // ── event handler (called by Kafka consumer) ──────────────────
    void processWalletEvent(WalletEvent event);

    // ── user preferences ─────────────────────────────────────────
    UserPreferenceDTO saveUserPreference(UserPreferenceDTO dto);
    UserPreferenceDTO getUserPreference(String userId);
    UserPreferenceDTO updateUserPreference(String userId, UserPreferenceDTO dto);

    // ── notification history ──────────────────────────────────────
    Page<NotificationLogDTO> getNotificationHistory(String userId, Pageable pageable);
    List<NotificationLogDTO> getNotificationsByDateRange(String userId, LocalDateTime start, LocalDateTime end);
    NotificationLogDTO getNotificationById(String notificationId);

    // ── retry failed notifications ────────────────────────────────
    void retryFailedNotifications();
}

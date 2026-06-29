package com.wallet.notification_service.repository;

import com.wallet.notification_service.entity.NotificationLog;
import com.wallet.notification_service.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Optional<NotificationLog> findByNotificationId(String notificationId);

    Page<NotificationLog> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<NotificationLog> findByStatusAndRetryCountLessThan(NotificationStatus status, int maxRetry);

    List<NotificationLog> findByUserIdAndCreatedAtBetween(
            String userId, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndStatus(String userId, NotificationStatus status);
}

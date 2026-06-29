package com.wallet.notification_service.controller;

import com.wallet.notification_service.dto.ApiResponse;
import com.wallet.notification_service.dto.NotificationLogDTO;
import com.wallet.notification_service.dto.UserPreferenceDTO;
import com.wallet.notification_service.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Management", description = "APIs for notification preferences and history")
public class NotificationController {

    private final NotificationService notificationService;

    // ==================== USER PREFERENCES ====================

    @PostMapping("/preferences")
    @Operation(summary = "Save user preferences",
               description = "Create or upsert notification preferences for a user")
    public ResponseEntity<ApiResponse<UserPreferenceDTO>> savePreferences(
            @Valid @RequestBody UserPreferenceDTO dto) {

        log.info("POST /api/notifications/preferences - userId: {}", dto.getUserId());
        UserPreferenceDTO saved = notificationService.saveUserPreference(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<UserPreferenceDTO>builder()
                        .success(true)
                        .message("Preferences saved successfully")
                        .data(saved)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @GetMapping("/preferences/{userId}")
    @Operation(summary = "Get user preferences")
    public ResponseEntity<ApiResponse<UserPreferenceDTO>> getPreferences(
            @PathVariable String userId) {

        log.info("GET /api/notifications/preferences/{}", userId);
        UserPreferenceDTO pref = notificationService.getUserPreference(userId);
        return ResponseEntity.ok(
                ApiResponse.<UserPreferenceDTO>builder()
                        .success(true)
                        .message("Preferences fetched successfully")
                        .data(pref)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @PutMapping("/preferences/{userId}")
    @Operation(summary = "Update user preferences")
    public ResponseEntity<ApiResponse<UserPreferenceDTO>> updatePreferences(
            @PathVariable String userId,
            @Valid @RequestBody UserPreferenceDTO dto) {

        log.info("PUT /api/notifications/preferences/{}", userId);
        UserPreferenceDTO updated = notificationService.updateUserPreference(userId, dto);
        return ResponseEntity.ok(
                ApiResponse.<UserPreferenceDTO>builder()
                        .success(true)
                        .message("Preferences updated successfully")
                        .data(updated)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== NOTIFICATION HISTORY ====================

    @GetMapping("/history/{userId}")
    @Operation(summary = "Get notification history",
               description = "Paginated notification history for a user")
    public ResponseEntity<ApiResponse<Page<NotificationLogDTO>>> getHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/notifications/history/{} page={} size={}", userId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationLogDTO> history = notificationService.getNotificationHistory(userId, pageable);
        return ResponseEntity.ok(
                ApiResponse.<Page<NotificationLogDTO>>builder()
                        .success(true)
                        .message("Notification history fetched successfully")
                        .data(history)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @GetMapping("/history/{userId}/date-range")
    @Operation(summary = "Get notifications by date range")
    public ResponseEntity<ApiResponse<List<NotificationLogDTO>>> getHistoryByDateRange(
            @PathVariable String userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("GET /api/notifications/history/{}/date-range {} to {}", userId, startDate, endDate);
        List<NotificationLogDTO> history =
                notificationService.getNotificationsByDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(
                ApiResponse.<List<NotificationLogDTO>>builder()
                        .success(true)
                        .message("Notifications fetched successfully")
                        .data(history)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<ApiResponse<NotificationLogDTO>> getById(
            @PathVariable String notificationId) {

        log.info("GET /api/notifications/{}", notificationId);
        NotificationLogDTO notification = notificationService.getNotificationById(notificationId);
        return ResponseEntity.ok(
                ApiResponse.<NotificationLogDTO>builder()
                        .success(true)
                        .message("Notification fetched successfully")
                        .data(notification)
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }

    // ==================== HEALTH CHECK ====================

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(
                ApiResponse.<String>builder()
                        .success(true)
                        .message("Notification service is running")
                        .data("OK")
                        .timestamp(System.currentTimeMillis())
                        .build()
        );
    }
}

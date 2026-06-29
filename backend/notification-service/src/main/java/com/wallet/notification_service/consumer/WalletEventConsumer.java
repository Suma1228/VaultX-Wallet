package com.wallet.notification_service.consumer;

import com.wallet.notification_service.event.WalletEvent;
import com.wallet.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consumes events from the wallet-events Kafka topic.
 *
 * Kafka flow:
 *   wallet-service  ──publishes──▶  wallet-events  ──consumed by──▶  WalletEventConsumer
 *                                                                            │
 *                                                                     NotificationService
 *                                                                            │
 *                                                                       EmailService
 *
 * Manual acknowledgment (MANUAL_IMMEDIATE) ensures the offset is committed
 * only after the notification is persisted. If the service crashes mid-processing,
 * Kafka will re-deliver the message.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics       = "${kafka.topics.wallet-events}",
            groupId      = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeWalletEvent(
            ConsumerRecord<String, WalletEvent> record,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        WalletEvent event = record.value();

        log.info("Received Kafka message | topic={} partition={} offset={} eventType={} userId={}",
                topic, partition, offset, event.getEventType(), event.getUserId());

        try {
            notificationService.processWalletEvent(event);
            acknowledgment.acknowledge();   // commit offset only on success
            log.info("Kafka message processed and acknowledged | offset={} eventType={}", offset, event.getEventType());

        } catch (Exception e) {
            // Do NOT acknowledge — Kafka will redeliver based on retry config.
            // In production, configure a Dead Letter Topic (DLT) via Spring Kafka's
            // DefaultErrorHandler + DeadLetterPublishingRecoverer.
            log.error("Failed to process Kafka message | offset={} eventType={} error={}",
                    offset, event.getEventType(), e.getMessage(), e);
            // Re-throw so Spring Kafka's error handler can apply backoff/DLT logic
            throw new RuntimeException("Event processing failed for eventType: " + event.getEventType(), e);
        }
    }
}

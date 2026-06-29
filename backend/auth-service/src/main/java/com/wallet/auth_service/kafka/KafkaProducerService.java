package com.wallet.auth_service.kafka;

import com.wallet.auth_service.dto.UserEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public void publishUserEvent(UserEvent event) {

        kafkaTemplate.send(
                KafkaTopics.USER_EVENTS,
                event.getUserId(),
                event
        );
    }
}
package com.wallet.wallet_service.kafka;

import com.wallet.wallet_service.event.WalletEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, WalletEvent> kafkaTemplate;

    public void publishWalletEvent(WalletEvent event) {

        kafkaTemplate.send(
                KafkaTopics.WALLET_EVENTS,
                event.getUserId(),
                event
        );
    }
}
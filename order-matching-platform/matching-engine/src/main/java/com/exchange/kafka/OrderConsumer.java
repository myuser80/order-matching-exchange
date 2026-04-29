package com.exchange.kafka;

import com.exchange.events.KafkaTopics;
import com.exchange.model.Order;
import com.exchange.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final MatchingService matchingService;

    /**
     * Listen to submitted orders, partitioned by ticker.
     * Each partition is consumed sequentially → ordering guarantee per ticker.
     * concurrency = number of Kafka partitions for horizontal scaling.
     */
    @KafkaListener(
            topics = KafkaTopics.ORDER_SUBMITTED,
            groupId = "matching-engine-group",
            concurrency = "3"
    )
    public void onOrderReceived(
            @Payload Order order,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received order {} from partition={} offset={}",
                order.getOrderId(), partition, offset);

        try {
            matchingService.processOrder(order);
        } catch (Exception e) {
            log.error("Error processing order {}: {}", order.getOrderId(), e.getMessage(), e);
            // In production: send to dead-letter topic
        }
    }
}

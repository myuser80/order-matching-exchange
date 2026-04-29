package com.exchange.kafka;

import com.exchange.events.KafkaTopics;
import com.exchange.model.Order;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, Order> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public void publishOrder(Order order) {
        // Key by ticker so same-ticker orders go to same partition (ordering guarantee)
        CompletableFuture<SendResult<String, Order>> future =
                kafkaTemplate.send(KafkaTopics.ORDER_SUBMITTED, order.getTicker(), order);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish order {}: {}", order.getOrderId(), ex.getMessage());
                meterRegistry.counter("orders.publish.failed",
                        "ticker", order.getTicker()).increment();
            } else {
                log.info("Order {} published to partition {} offset {}",
                        order.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                meterRegistry.counter("orders.published",
                        "ticker", order.getTicker(),
                        "side", order.getSide().name()).increment();
            }
        });
    }
}

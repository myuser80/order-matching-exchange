package com.exchange.kafka;

import com.exchange.events.KafkaTopics;
import com.exchange.model.Trade;
import com.exchange.service.ExecutionService;
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
public class TradeConsumer {

    private final ExecutionService executionService;

    @KafkaListener(
            topics = KafkaTopics.TRADE_EXECUTED,
            groupId = "execution-service-group",
            concurrency = "3"
    )
    public void onTradeExecuted(
            @Payload Trade trade,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Trade received: {} from partition={} offset={}",
                trade.getTradeId(), partition, offset);

        try {
            executionService.processTrade(trade);
        } catch (Exception e) {
            log.error("Failed to process trade {}: {}", trade.getTradeId(), e.getMessage(), e);
        }
    }
}

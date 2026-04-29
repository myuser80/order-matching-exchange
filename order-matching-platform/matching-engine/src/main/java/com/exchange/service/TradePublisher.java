package com.exchange.service;

import com.exchange.events.KafkaTopics;
import com.exchange.model.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradePublisher {

    private final KafkaTemplate<String, Trade> tradeKafkaTemplate;

    public void publishTrade(Trade trade) {
        tradeKafkaTemplate.send(KafkaTopics.TRADE_EXECUTED, trade.getTicker(), trade)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish trade {}: {}", trade.getTradeId(), ex.getMessage());
                    } else {
                        log.info("Trade {} published → partition {} offset {}",
                                trade.getTradeId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}

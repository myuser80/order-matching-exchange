package com.exchange.service;

import com.exchange.engine.MatchingAlgorithm;
import com.exchange.model.Order;
import com.exchange.model.Trade;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final MatchingAlgorithm matchingAlgorithm;
    private final TradePublisher tradePublisher;
    private final MeterRegistry meterRegistry;

    public List<Trade> processOrder(Order order) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            log.info("Processing order: {} {} {} @ {} qty={}",
                    order.getOrderId(), order.getSide(),
                    order.getTicker(), order.getLimitPrice(), order.getQuantity());

            List<Trade> trades = matchingAlgorithm.match(order);

            // Publish each trade to Kafka
            for (Trade trade : trades) {
                tradePublisher.publishTrade(trade);
                meterRegistry.counter("trades.executed",
                        "ticker", trade.getTicker()).increment();
            }

            meterRegistry.counter("orders.processed",
                    "ticker", order.getTicker(),
                    "side", order.getSide().name(),
                    "status", order.getStatus().name()).increment();

            log.info("Order {} processed. Status={} FilledQty={} Trades={}",
                    order.getOrderId(), order.getStatus(),
                    order.getFilledQuantity(), trades.size());

            return trades;

        } finally {
            sample.stop(Timer.builder("order.matching.duration")
                    .tag("ticker", order.getTicker())
                    .register(meterRegistry));
        }
    }
}

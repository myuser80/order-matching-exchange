package com.exchange.service;

import com.exchange.entity.TradeEntity;
import com.exchange.model.Trade;
import com.exchange.repository.TradeRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final TradeRepository tradeRepository;
    private final MeterRegistry meterRegistry;

    @Transactional
    public void processTrade(Trade trade) {
        log.info("Processing trade: {} {} @ {} qty={}",
                trade.getTradeId(), trade.getTicker(),
                trade.getExecutionPrice(), trade.getExecutedQuantity());

        // Persist trade
        TradeEntity entity = TradeEntity.builder()
                .tradeId(trade.getTradeId())
                .ticker(trade.getTicker())
                .buyOrderId(trade.getBuyOrderId())
                .sellOrderId(trade.getSellOrderId())
                .buyClientId(trade.getBuyClientId())
                .sellClientId(trade.getSellClientId())
                .executionPrice(trade.getExecutionPrice())
                .executedQuantity(trade.getExecutedQuantity())
                .executedAt(trade.getExecutedAt())
                .build();

        tradeRepository.save(entity);

        // Record metrics
        meterRegistry.counter("trades.persisted", "ticker", trade.getTicker()).increment();
        meterRegistry.summary("trade.volume", "ticker", trade.getTicker())
                .record(trade.getExecutedQuantity());

        log.info("Trade persisted: {} | Buyer: {} | Seller: {} | Qty: {} @ {}",
                trade.getTradeId(),
                trade.getBuyClientId(),
                trade.getSellClientId(),
                trade.getExecutedQuantity(),
                trade.getExecutionPrice());

        // In production: also send execution reports via WebSocket/FIX to each client
        notifyCounterparties(trade);
    }

    private void notifyCounterparties(Trade trade) {
        // Simulate notification (in production: push via WebSocket/FIX gateway)
        log.info("EXECUTION REPORT → Buyer [{}]: Bought {} {} @ {}",
                trade.getBuyClientId(), trade.getExecutedQuantity(),
                trade.getTicker(), trade.getExecutionPrice());
        log.info("EXECUTION REPORT → Seller [{}]: Sold {} {} @ {}",
                trade.getSellClientId(), trade.getExecutedQuantity(),
                trade.getTicker(), trade.getExecutionPrice());
    }

    public List<TradeEntity> getTradesByTicker(String ticker) {
        return tradeRepository.findByTicker(ticker);
    }

    public List<TradeEntity> getTradesByClient(String clientId) {
        return tradeRepository.findByBuyClientIdOrSellClientId(clientId, clientId);
    }

    public List<TradeEntity> getAllTrades() {
        return tradeRepository.findAll();
    }
}

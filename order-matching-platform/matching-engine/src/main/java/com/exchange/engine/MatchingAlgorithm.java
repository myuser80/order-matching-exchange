package com.exchange.engine;

import com.exchange.enums.OrderSide;
import com.exchange.enums.OrderStatus;
import com.exchange.enums.TimeInForce;
import com.exchange.model.Order;
import com.exchange.model.Trade;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MatchingAlgorithm implements the Price-Time Priority (FIFO) matching.
 *
 * How it works (like the canteen example):
 * 1. A new order arrives
 * 2. We check the opposite side of the book for a matching price
 * 3. If BUY price >= SELL price → TRADE! Execute at the resting order's price
 * 4. Handle partial fills (big order, small counter-order)
 * 5. Emit Trade objects for each match
 */
@Slf4j
public class MatchingAlgorithm {

    // One OrderBook per ticker symbol
    private final ConcurrentHashMap<String, OrderBook> books = new ConcurrentHashMap<>();

    /**
     * Match an incoming order against the book.
     * Returns list of trades created.
     */
    public synchronized List<Trade> match(Order incomingOrder) {
        String ticker = incomingOrder.getTicker();
        OrderBook book = books.computeIfAbsent(ticker, OrderBook::new);

        List<Trade> trades = new ArrayList<>();

        // IOC: try to match immediately, cancel remaining
        // FOK: fill entire quantity or cancel completely
        if (incomingOrder.getTimeInForce() == TimeInForce.FOK) {
            if (!canFillEntirely(book, incomingOrder)) {
                incomingOrder.setStatus(OrderStatus.CANCELLED);
                log.info("FOK order {} cancelled - insufficient liquidity", incomingOrder.getOrderId());
                return trades;
            }
        }

        // Try to match against opposite side
        while (incomingOrder.getRemainingQuantity() > 0) {
            Order restingOrder = getOppositeOrder(book, incomingOrder);
            if (restingOrder == null) break;

            // Check price compatibility
            if (!pricesMatch(incomingOrder, restingOrder)) break;

            // Determine execution price: resting order's price (price-time priority)
            BigDecimal executionPrice = restingOrder.getLimitPrice();

            // Determine fill quantity
            long fillQty = Math.min(
                    incomingOrder.getRemainingQuantity(),
                    restingOrder.getRemainingQuantity()
            );

            // Build trade
            Trade trade = buildTrade(incomingOrder, restingOrder, executionPrice, fillQty, ticker);
            trades.add(trade);

            log.info("TRADE: {} {} @ {} qty={} buyer={} seller={}",
                    ticker, executionPrice, executionPrice, fillQty,
                    trade.getBuyClientId(), trade.getSellClientId());

            // Update fill quantities
            incomingOrder.setFilledQuantity(incomingOrder.getFilledQuantity() + fillQty);
            restingOrder.setFilledQuantity(restingOrder.getFilledQuantity() + fillQty);

            // Update statuses
            updateStatus(incomingOrder);
            updateStatus(restingOrder);

            // If resting order fully filled, remove from book
            if (restingOrder.isFullyFilled()) {
                book.removeOrder(restingOrder);
            } else {
                // Resting order partially filled but still in book
                // (already in position, quantity updated in-place)
            }
        }

        // Handle IOC: cancel unfilled portion
        if (incomingOrder.getTimeInForce() == TimeInForce.IOC
                && incomingOrder.getRemainingQuantity() > 0
                && incomingOrder.getStatus() != OrderStatus.FILLED) {
            incomingOrder.setStatus(OrderStatus.CANCELLED);
            log.info("IOC order {} partially matched, remainder cancelled", incomingOrder.getOrderId());
        }

        // GTC: add unfilled portion to book
        if (incomingOrder.getTimeInForce() == TimeInForce.GTC
                && incomingOrder.getRemainingQuantity() > 0
                && incomingOrder.getStatus() != OrderStatus.FILLED) {
            book.addOrder(incomingOrder);
            log.info("GTC order {} resting in book, remaining qty={}",
                    incomingOrder.getOrderId(), incomingOrder.getRemainingQuantity());
        }

        return trades;
    }

    private Order getOppositeOrder(OrderBook book, Order incoming) {
        if (incoming.getSide() == OrderSide.BUY) {
            return book.bestAsk().orElse(null);
        } else {
            return book.bestBid().orElse(null);
        }
    }

    private boolean pricesMatch(Order incoming, Order resting) {
        if (incoming.getSide() == OrderSide.BUY) {
            // Buyer will pay up to incoming.limitPrice; seller wants at least resting.limitPrice
            return incoming.getLimitPrice().compareTo(resting.getLimitPrice()) >= 0;
        } else {
            // Seller will accept as low as incoming.limitPrice; buyer bids resting.limitPrice
            return resting.getLimitPrice().compareTo(incoming.getLimitPrice()) >= 0;
        }
    }

    private boolean canFillEntirely(OrderBook book, Order order) {
        long needed = order.getQuantity();
        // Sum up available liquidity on opposite side at matching prices
        // Simplified check — production would iterate price levels
        if (order.getSide() == OrderSide.BUY) {
            return book.getTotalAskQuantity() >= needed;
        } else {
            return book.getTotalBidQuantity() >= needed;
        }
    }

    private void updateStatus(Order order) {
        if (order.isFullyFilled()) {
            order.setStatus(OrderStatus.FILLED);
        } else if (order.getFilledQuantity() > 0) {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }
        order.setUpdatedAt(Instant.now());
    }

    private Trade buildTrade(Order incoming, Order resting,
                              BigDecimal price, long qty, String ticker) {
        Order buyOrder  = incoming.getSide() == OrderSide.BUY ? incoming : resting;
        Order sellOrder = incoming.getSide() == OrderSide.SELL ? incoming : resting;

        return Trade.builder()
                .tradeId(UUID.randomUUID().toString())
                .ticker(ticker)
                .buyOrderId(buyOrder.getOrderId())
                .sellOrderId(sellOrder.getOrderId())
                .buyClientId(buyOrder.getClientId())
                .sellClientId(sellOrder.getClientId())
                .executionPrice(price)
                .executedQuantity(qty)
                .executedAt(Instant.now())
                .build();
    }

    public OrderBook getBook(String ticker) {
        return books.get(ticker);
    }
}

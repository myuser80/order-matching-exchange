package com.exchange.engine;

import com.exchange.enums.OrderSide;
import com.exchange.model.Order;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * OrderBook maintains sorted buy (bids) and sell (asks) queues for a single ticker.
 *
 * Price-Time Priority:
 *   - BUY  orders: highest price first (we want to buy as cheaply as possible,
 *                  but a buyer willing to pay MORE is prioritised)
 *   - SELL orders: lowest price first (seller willing to accept LESS gets priority)
 *
 * Think of it like an auction:
 *   - Best BID = highest price a buyer is willing to pay
 *   - Best ASK = lowest price a seller will accept
 *   - Trade happens when BID >= ASK
 */
@Slf4j
public class OrderBook {

    private final String ticker;

    // BUY side: highest price first → natural reverse order
    // Each price level has a queue of orders (FIFO = time priority)
    private final TreeMap<BigDecimal, Deque<Order>> bids =
            new TreeMap<>(Comparator.reverseOrder());

    // SELL side: lowest price first → natural ascending order
    private final TreeMap<BigDecimal, Deque<Order>> asks =
            new TreeMap<>();

    public OrderBook(String ticker) {
        this.ticker = ticker;
    }

    public synchronized void addOrder(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            bids.computeIfAbsent(order.getLimitPrice(), k -> new ArrayDeque<>())
                .addLast(order);
        } else {
            asks.computeIfAbsent(order.getLimitPrice(), k -> new ArrayDeque<>())
                .addLast(order);
        }
        log.debug("[{}] Added {} order {} @ {} qty={}",
                ticker, order.getSide(), order.getOrderId(),
                order.getLimitPrice(), order.getQuantity());
    }

    public synchronized void removeOrder(Order order) {
        TreeMap<BigDecimal, Deque<Order>> book =
                order.getSide() == OrderSide.BUY ? bids : asks;
        Deque<Order> queue = book.get(order.getLimitPrice());
        if (queue != null) {
            queue.remove(order);
            if (queue.isEmpty()) {
                book.remove(order.getLimitPrice());
            }
        }
    }

    /**
     * Returns the best buy order (highest bidder).
     */
    public synchronized Optional<Order> bestBid() {
        if (bids.isEmpty()) return Optional.empty();
        Deque<Order> queue = bids.firstEntry().getValue();
        return queue.isEmpty() ? Optional.empty() : Optional.of(queue.peekFirst());
    }

    /**
     * Returns the best sell order (lowest asker).
     */
    public synchronized Optional<Order> bestAsk() {
        if (asks.isEmpty()) return Optional.empty();
        Deque<Order> queue = asks.firstEntry().getValue();
        return queue.isEmpty() ? Optional.empty() : Optional.of(queue.peekFirst());
    }

    /**
     * Checks if a trade is possible: best bid price >= best ask price.
     */
    public synchronized boolean canMatch() {
        Optional<Order> bid = bestBid();
        Optional<Order> ask = bestAsk();
        if (bid.isEmpty() || ask.isEmpty()) return false;
        return bid.get().getLimitPrice().compareTo(ask.get().getLimitPrice()) >= 0;
    }

    /**
     * Removes the front order from the buy queue at a given price.
     */
    public synchronized Order pollBestBid() {
        Map.Entry<BigDecimal, Deque<Order>> entry = bids.firstEntry();
        if (entry == null) return null;
        Order order = entry.getValue().pollFirst();
        if (entry.getValue().isEmpty()) bids.remove(entry.getKey());
        return order;
    }

    /**
     * Removes the front order from the sell queue at a given price.
     */
    public synchronized Order pollBestAsk() {
        Map.Entry<BigDecimal, Deque<Order>> entry = asks.firstEntry();
        if (entry == null) return null;
        Order order = entry.getValue().pollFirst();
        if (entry.getValue().isEmpty()) asks.remove(entry.getKey());
        return order;
    }

    public String getTicker() { return ticker; }

    public synchronized int getBidLevels() { return bids.size(); }
    public synchronized int getAskLevels() { return asks.size(); }

    public synchronized long getTotalBidQuantity() {
        return bids.values().stream()
                .flatMap(Collection::stream)
                .mapToLong(Order::getRemainingQuantity).sum();
    }

    public synchronized long getTotalAskQuantity() {
        return asks.values().stream()
                .flatMap(Collection::stream)
                .mapToLong(Order::getRemainingQuantity).sum();
    }
}

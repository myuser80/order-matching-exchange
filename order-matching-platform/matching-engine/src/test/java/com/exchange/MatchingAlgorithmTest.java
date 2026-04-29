package com.exchange;

import com.exchange.engine.MatchingAlgorithm;
import com.exchange.enums.OrderSide;
import com.exchange.enums.OrderStatus;
import com.exchange.enums.TimeInForce;
import com.exchange.model.Order;
import com.exchange.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the Matching Algorithm.
 * No Spring context needed — pure unit tests.
 */
class MatchingAlgorithmTest {

    private MatchingAlgorithm engine;

    @BeforeEach
    void setUp() {
        engine = new MatchingAlgorithm();
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private Order order(String clientId, OrderSide side, double price, long qty,
                        TimeInForce tif) {
        return Order.builder()
                .orderId(UUID.randomUUID().toString())
                .clientId(clientId)
                .ticker("RELIANCE")
                .side(side)
                .limitPrice(BigDecimal.valueOf(price))
                .quantity(qty)
                .filledQuantity(0L)
                .timeInForce(tif)
                .status(OrderStatus.NEW)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ─── Tests ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Basic Matching")
    class BasicMatching {

        @Test
        @DisplayName("BUY and SELL at same price → should match fully")
        void fullMatchAtSamePrice() {
            Order sell = order("SELLER1", OrderSide.SELL, 100.0, 10, TimeInForce.GTC);
            Order buy  = order("BUYER1",  OrderSide.BUY,  100.0, 10, TimeInForce.GTC);

            // Add sell to book first
            engine.match(sell);
            // Buy comes in — should match
            List<Trade> trades = engine.match(buy);

            assertThat(trades).hasSize(1);
            Trade trade = trades.get(0);
            assertThat(trade.getExecutedQuantity()).isEqualTo(10);
            assertThat(trade.getExecutionPrice()).isEqualByComparingTo("100.0");
            assertThat(trade.getBuyClientId()).isEqualTo("BUYER1");
            assertThat(trade.getSellClientId()).isEqualTo("SELLER1");
            assertThat(buy.getStatus()).isEqualTo(OrderStatus.FILLED);
            assertThat(sell.getStatus()).isEqualTo(OrderStatus.FILLED);
        }

        @Test
        @DisplayName("BUY price > SELL price → should match at SELL price (resting order)")
        void buyHigherThanSell_matchesAtSellPrice() {
            Order sell = order("SELLER1", OrderSide.SELL, 95.0,  10, TimeInForce.GTC);
            Order buy  = order("BUYER1",  OrderSide.BUY,  100.0, 10, TimeInForce.GTC);

            engine.match(sell); // sell rests at 95
            List<Trade> trades = engine.match(buy); // buy at 100 matches sell at 95

            assertThat(trades).hasSize(1);
            // Execution at resting order's price (price-time priority)
            assertThat(trades.get(0).getExecutionPrice()).isEqualByComparingTo("95.0");
        }

        @Test
        @DisplayName("BUY price < SELL price → no match, both rest in book")
        void noMatchWhenPricesDontCross() {
            Order sell = order("SELLER1", OrderSide.SELL, 105.0, 10, TimeInForce.GTC);
            Order buy  = order("BUYER1",  OrderSide.BUY,  95.0,  10, TimeInForce.GTC);

            List<Trade> sellTrades = engine.match(sell);
            List<Trade> buyTrades  = engine.match(buy);

            assertThat(sellTrades).isEmpty();
            assertThat(buyTrades).isEmpty();
            assertThat(buy.getStatus()).isEqualTo(OrderStatus.NEW);
            assertThat(sell.getStatus()).isEqualTo(OrderStatus.NEW);
        }
    }

    @Nested
    @DisplayName("Partial Fills")
    class PartialFills {

        @Test
        @DisplayName("Large BUY hits smaller SELL → BUY partially filled")
        void largeBuyPartiallyFilled() {
            Order sell = order("SELLER1", OrderSide.SELL, 100.0, 5,  TimeInForce.GTC);
            Order buy  = order("BUYER1",  OrderSide.BUY,  100.0, 10, TimeInForce.GTC);

            engine.match(sell);
            List<Trade> trades = engine.match(buy);

            assertThat(trades).hasSize(1);
            assertThat(trades.get(0).getExecutedQuantity()).isEqualTo(5);
            assertThat(buy.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
            assertThat(buy.getFilledQuantity()).isEqualTo(5);
            assertThat(buy.getRemainingQuantity()).isEqualTo(5);
            assertThat(sell.getStatus()).isEqualTo(OrderStatus.FILLED);
        }

        @Test
        @DisplayName("Large BUY hits multiple SELLs → aggregated fills")
        void largeBuyHitsMultipleSells() {
            Order sell1 = order("SELLER1", OrderSide.SELL, 100.0, 5, TimeInForce.GTC);
            Order sell2 = order("SELLER2", OrderSide.SELL, 100.0, 5, TimeInForce.GTC);
            Order buy   = order("BUYER1",  OrderSide.BUY,  100.0, 10, TimeInForce.GTC);

            engine.match(sell1);
            engine.match(sell2);
            List<Trade> trades = engine.match(buy);

            assertThat(trades).hasSize(2);
            assertThat(buy.getStatus()).isEqualTo(OrderStatus.FILLED);
            long totalFilled = trades.stream()
                    .mapToLong(Trade::getExecutedQuantity).sum();
            assertThat(totalFilled).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Time-In-Force")
    class TimeInForceTests {

        @Test
        @DisplayName("IOC with partial fill → remainder cancelled")
        void iocPartialFillCancelsRemainder() {
            Order sell = order("SELLER1", OrderSide.SELL, 100.0, 3,  TimeInForce.GTC);
            Order buy  = order("BUYER1",  OrderSide.BUY,  100.0, 10, TimeInForce.IOC);

            engine.match(sell);
            List<Trade> trades = engine.match(buy);

            assertThat(trades).hasSize(1);
            assertThat(trades.get(0).getExecutedQuantity()).isEqualTo(3);
            assertThat(buy.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("FOK with insufficient liquidity → fully cancelled")
        void fokCancelledWhenInsufficientLiquidity() {
            Order sell = order("SELLER1", OrderSide.SELL, 100.0, 3,  TimeInForce.GTC);
            Order buy  = order("BUYER1",  OrderSide.BUY,  100.0, 10, TimeInForce.FOK);

            engine.match(sell);
            List<Trade> trades = engine.match(buy);

            // FOK requires all 10 but only 3 available → cancel with 0 trades
            assertThat(trades).isEmpty();
            assertThat(buy.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("FOK with sufficient liquidity → fully filled")
        void fokFilledWhenSufficientLiquidity() {
            Order sell = order("SELLER1", OrderSide.SELL, 100.0, 10, TimeInForce.GTC);
            Order buy  = order("BUYER1",  OrderSide.BUY,  100.0, 10, TimeInForce.FOK);

            engine.match(sell);
            List<Trade> trades = engine.match(buy);

            assertThat(trades).hasSize(1);
            assertThat(buy.getStatus()).isEqualTo(OrderStatus.FILLED);
        }
    }

    @Nested
    @DisplayName("Price-Time Priority")
    class PriceTimePriority {

        @Test
        @DisplayName("Best priced SELL matched first")
        void bestPricedSellMatchedFirst() {
            // Two sellers: one at 98, one at 100
            Order sellExpensive = order("SELLER_EXP",   OrderSide.SELL, 100.0, 5, TimeInForce.GTC);
            Order sellCheap     = order("SELLER_CHEAP",  OrderSide.SELL,  98.0, 5, TimeInForce.GTC);

            engine.match(sellExpensive); // rests at 100
            engine.match(sellCheap);     // rests at 98 — BETTER for buyer

            Order buy = order("BUYER1", OrderSide.BUY, 100.0, 5, TimeInForce.GTC);
            List<Trade> trades = engine.match(buy);

            assertThat(trades).hasSize(1);
            // Should match with the cheaper seller
            assertThat(trades.get(0).getSellClientId()).isEqualTo("SELLER_CHEAP");
            assertThat(trades.get(0).getExecutionPrice()).isEqualByComparingTo("98.0");
        }
    }
}

package com.exchange.model;

import com.exchange.enums.OrderSide;
import com.exchange.enums.OrderStatus;
import com.exchange.enums.TimeInForce;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private String orderId;
    private String clientId;
    private String ticker;       // e.g., RELIANCE, TCS
    private String isin;         // e.g., INE002A01018
    private OrderSide side;      // BUY or SELL
    private BigDecimal limitPrice;
    private long quantity;
    private long filledQuantity;
    private TimeInForce timeInForce;
    private OrderStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;

    public long getRemainingQuantity() {
        return quantity - filledQuantity;
    }

    public boolean isFullyFilled() {
        return filledQuantity >= quantity;
    }
}

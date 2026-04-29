package com.exchange.entity;

import com.exchange.enums.OrderSide;
import com.exchange.enums.OrderStatus;
import com.exchange.enums.TimeInForce;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    @Id
    private String orderId;

    @Column(nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String ticker;

    private String isin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal limitPrice;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false)
    private long filledQuantity;

    @Enumerated(EnumType.STRING)
    private TimeInForce timeInForce;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Instant createdAt;
    private Instant updatedAt;
}

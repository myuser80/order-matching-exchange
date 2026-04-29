package com.exchange.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trades")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeEntity {

    @Id
    private String tradeId;

    @Column(nullable = false)
    private String ticker;

    private String buyOrderId;
    private String sellOrderId;
    private String buyClientId;
    private String sellClientId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal executionPrice;

    @Column(nullable = false)
    private long executedQuantity;

    private Instant executedAt;
}

package com.exchange.model;

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
public class Trade {
    private String tradeId;
    private String ticker;
    private String buyOrderId;
    private String sellOrderId;
    private String buyClientId;
    private String sellClientId;
    private BigDecimal executionPrice;
    private long executedQuantity;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant executedAt;
}

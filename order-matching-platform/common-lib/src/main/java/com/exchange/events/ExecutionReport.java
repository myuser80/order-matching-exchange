package com.exchange.events;

import com.exchange.enums.OrderSide;
import com.exchange.enums.OrderStatus;
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
public class ExecutionReport {
    private String executionId;
    private String tradeId;
    private String orderId;
    private String clientId;
    private String ticker;
    private OrderSide side;
    private BigDecimal executionPrice;
    private long executedQuantity;
    private long cumulativeQuantity;
    private long leavesQuantity;
    private OrderStatus orderStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
}

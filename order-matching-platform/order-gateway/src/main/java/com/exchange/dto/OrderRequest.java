package com.exchange.dto;

import com.exchange.enums.OrderSide;
import com.exchange.enums.TimeInForce;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderRequest {

    @NotBlank(message = "clientId is required")
    private String clientId;

    @NotBlank(message = "ticker is required")
    private String ticker;

    private String isin;

    @NotNull(message = "side is required (BUY or SELL)")
    private OrderSide side;

    @NotNull(message = "limitPrice is required")
    @DecimalMin(value = "0.01", message = "limitPrice must be positive")
    private BigDecimal limitPrice;

    @Min(value = 1, message = "quantity must be at least 1")
    private long quantity;

    @NotNull(message = "timeInForce is required (GTC, IOC, FOK)")
    private TimeInForce timeInForce;
}

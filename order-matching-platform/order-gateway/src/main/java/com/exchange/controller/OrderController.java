package com.exchange.controller;

import com.exchange.dto.OrderRequest;
import com.exchange.entity.OrderEntity;
import com.exchange.model.Order;
import com.exchange.service.OrderService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Submit a new BUY or SELL order.
     * POST /api/v1/orders
     */
    @PostMapping
    @Timed(value = "order.submit.latency", description = "Time to submit an order")
    public ResponseEntity<Order> submitOrder(@Valid @RequestBody OrderRequest request) {
        Order order = orderService.submitOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Get order by ID.
     * GET /api/v1/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderEntity> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    /**
     * Get all orders for a client.
     * GET /api/v1/orders?clientId=CLIENT1
     */
    @GetMapping
    public ResponseEntity<List<OrderEntity>> getOrdersByClient(
            @RequestParam String clientId) {
        return ResponseEntity.ok(orderService.getOrdersByClient(clientId));
    }

    /**
     * Health check / info endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "order-gateway"));
    }
}

package com.exchange.service;

import com.exchange.dto.OrderRequest;
import com.exchange.entity.OrderEntity;
import com.exchange.enums.OrderStatus;
import com.exchange.kafka.OrderProducer;
import com.exchange.model.Order;
import com.exchange.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderProducer orderProducer;

    @Transactional
    public Order submitOrder(OrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        // Persist order first (it's NEW)
        OrderEntity entity = OrderEntity.builder()
                .orderId(orderId)
                .clientId(request.getClientId())
                .ticker(request.getTicker().toUpperCase())
                .isin(request.getIsin())
                .side(request.getSide())
                .limitPrice(request.getLimitPrice())
                .quantity(request.getQuantity())
                .filledQuantity(0L)
                .timeInForce(request.getTimeInForce())
                .status(OrderStatus.NEW)
                .createdAt(now)
                .updatedAt(now)
                .build();

        orderRepository.save(entity);

        // Build domain model and publish to Kafka
        Order order = Order.builder()
                .orderId(orderId)
                .clientId(request.getClientId())
                .ticker(request.getTicker().toUpperCase())
                .isin(request.getIsin())
                .side(request.getSide())
                .limitPrice(request.getLimitPrice())
                .quantity(request.getQuantity())
                .filledQuantity(0L)
                .timeInForce(request.getTimeInForce())
                .status(OrderStatus.NEW)
                .createdAt(now)
                .updatedAt(now)
                .build();

        orderProducer.publishOrder(order);

        log.info("Order submitted: {} {} {} @ {} qty={}",
                orderId, order.getSide(), order.getTicker(),
                order.getLimitPrice(), order.getQuantity());

        return order;
    }

    public List<OrderEntity> getOrdersByClient(String clientId) {
        return orderRepository.findByClientId(clientId);
    }

    public OrderEntity getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }
}

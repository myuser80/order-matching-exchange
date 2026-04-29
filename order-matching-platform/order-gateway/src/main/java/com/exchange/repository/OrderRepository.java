package com.exchange.repository;

import com.exchange.entity.OrderEntity;
import com.exchange.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    List<OrderEntity> findByClientId(String clientId);
    List<OrderEntity> findByTickerAndStatus(String ticker, OrderStatus status);
}

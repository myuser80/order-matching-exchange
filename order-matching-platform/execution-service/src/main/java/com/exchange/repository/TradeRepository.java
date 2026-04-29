package com.exchange.repository;

import com.exchange.entity.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<TradeEntity, String> {
    List<TradeEntity> findByTicker(String ticker);
    List<TradeEntity> findByBuyClientIdOrSellClientId(String buyClientId, String sellClientId);
}

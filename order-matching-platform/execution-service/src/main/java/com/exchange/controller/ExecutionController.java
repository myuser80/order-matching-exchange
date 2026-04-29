package com.exchange.controller;

import com.exchange.entity.TradeEntity;
import com.exchange.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    /**
     * Get all trades for a ticker.
     * GET /api/v1/executions/trades?ticker=RELIANCE
     */
    @GetMapping("/trades")
    public ResponseEntity<List<TradeEntity>> getTradesByTicker(
            @RequestParam String ticker) {
        return ResponseEntity.ok(executionService.getTradesByTicker(ticker));
    }

    /**
     * Get all trades for a client.
     * GET /api/v1/executions/client?clientId=CLIENT1
     */
    @GetMapping("/client")
    public ResponseEntity<List<TradeEntity>> getTradesByClient(
            @RequestParam String clientId) {
        return ResponseEntity.ok(executionService.getTradesByClient(clientId));
    }

    /**
     * Get all trades.
     * GET /api/v1/executions/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<TradeEntity>> getAllTrades() {
        return ResponseEntity.ok(executionService.getAllTrades());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "execution-service"));
    }
}

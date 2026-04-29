package com.exchange.config;

import com.exchange.engine.MatchingAlgorithm;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineConfig {

    @Bean
    public MatchingAlgorithm matchingAlgorithm() {
        return new MatchingAlgorithm();
    }
}

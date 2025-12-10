package com.hft.aggregator.routing;

import com.hft.aggregator.domain.LiquiditySource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Scores and selects optimal liquidity sources.
 */
@Slf4j
@Component
public class LiquidityScorer {

    private final Map<String, LiquiditySource> sources = new ConcurrentHashMap<>();
    private final double latencyWeight = 0.3;
    private final double reliabilityWeight = 0.5;
    private final double feeWeight = 0.2;

    /**
     * Register a liquidity source.
     */
    public void registerSource(LiquiditySource source) {
        sources.put(source.getId(), source);
        log.info("Registered liquidity source: {} ({})", source.getId(), source.getType());
    }

    /**
     * Select best sources for an order.
     */
    public List<LiquiditySource> selectSources(String symbol, double quantity, boolean isBuy) {
        return sources.values().stream()
                .filter(LiquiditySource::isHealthy)
                .sorted((a, b) -> Double.compare(
                        b.calculateScore(latencyWeight, reliabilityWeight),
                        a.calculateScore(latencyWeight, reliabilityWeight)))
                .limit(5) // Top 5 sources
                .collect(Collectors.toList());
    }

    /**
     * Get source by ID.
     */
    public LiquiditySource getSource(String sourceId) {
        return sources.get(sourceId);
    }
}

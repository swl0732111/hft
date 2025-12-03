package com.hft.trading.engine;

import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.Object2LongHashMap;
import org.springframework.stereotype.Component;

/**
 * Tracks last trade price for each symbol.
 * Used by StopLimitOrderHandler to monitor trigger conditions.
 * Uses Agrona collections for zero-allocation performance.
 */
@Slf4j
@Component
public class MarketPriceTracker {
    // symbol -> last trade price (scaled)
    // Using Agrona for zero-allocation (no boxing of long values)
    private final Object2LongHashMap<String> lastPrices = new Object2LongHashMap<>(0L);

    /**
     * Update last trade price for a symbol.
     */
    public void updatePrice(String symbol, long priceScaled) {
        lastPrices.put(symbol, priceScaled);
        log.trace("Market price updated: symbol={}, price={}", symbol, priceScaled);
    }

    /**
     * Get last trade price for a symbol.
     * Returns 0 if no trades yet.
     */
    public long getLastPrice(String symbol) {
        return lastPrices.getValue(symbol);
    }

    /**
     * Check if price exists for symbol.
     */
    public boolean hasPrice(String symbol) {
        return lastPrices.containsKey(symbol);
    }
}

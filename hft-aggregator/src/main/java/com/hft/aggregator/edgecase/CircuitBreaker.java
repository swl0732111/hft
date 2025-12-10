package com.hft.aggregator.edgecase;

import com.hft.aggregator.domain.ArrayOrderBook;
import com.hft.aggregator.domain.OrderBookLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit Breaker for HFT operations.
 * Monitors market conditions and trips if anomalies are detected.
 */
@Slf4j
@Component
public class CircuitBreaker {

    public enum State {
        CLOSED, // Normal operation
        OPEN, // Circuit tripped, reject orders
        HALF_OPEN // Testing recovery
    }

    @Getter
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicLong lastTripTime = new AtomicLong(0);
    private final long recoveryTimeoutMs = 10_000; // 10 seconds to attempt recovery

    // Thresholds
    private static final double MAX_PRICE_DROP_PERCENT = 0.05; // 5% drop
    private static final double MIN_DEPTH_VALUE = 1000.0; // Minimum notional value for liquidity

    // State for price deviation check
    private final java.util.Map<String, Double> lastValidPrices = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, Long> lastPriceUpdateTime = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long PRICE_UPDATE_INTERVAL_MS = 1000; // Update reference price every 1s

    /**
     * Check if operations are allowed.
     */
    public boolean allowRequest() {
        State currentState = state.get();
        if (currentState == State.CLOSED) {
            return true;
        }

        if (currentState == State.OPEN) {
            long now = System.currentTimeMillis();
            if (now - lastTripTime.get() > recoveryTimeoutMs) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    log.info("Circuit breaker entering HALF_OPEN state");
                    return true;
                }
            }
            return false;
        }

        return currentState == State.HALF_OPEN;
    }

    /**
     * Report successful execution (to reset HALF_OPEN).
     */
    public void onSuccess() {
        if (state.get() == State.HALF_OPEN) {
            if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                log.info("Circuit breaker reset to CLOSED");
            }
        }
    }

    /**
     * Report failure (to trip breaker).
     */
    public void onFailure(String reason) {
        trip(reason);
    }

    /**
     * Inspect order book/market data for anomalies.
     */
    /**
     * Inspect order book/market data for anomalies.
     * Zero-allocation version using ArrayOrderBook.
     */
    public void checkMarketConditions(com.hft.aggregator.domain.ArrayOrderBook book) {
        if (state.get() == State.OPEN)
            return;

        // Resolve symbol from ID
        String symbol = com.hft.aggregator.domain.SymbolDictionary.getSymbol(book.getSymbolId());
        if (symbol == null)
            return; // Should not happen

        // 1. Check for empty book
        if (book.getBidCount() == 0 || book.getAskCount() == 0) {
            trip("Empty order book for " + symbol);
            return;
        }

        long bestBidPrice = book.getBestBidPrice();
        long bestAskPrice = book.getBestAskPrice();

        // 2. Check spread
        // Prices are scaled (long), so spread check needs to handle that.
        // Logic: Ask - Bid.
        long spread = bestAskPrice - bestBidPrice;
        if (spread <= 0) {
            trip("Crossed/Locked market (Spread: " + spread + ") for " + symbol);
            return;
        }

        // 3. Check Minimum Liquidity (Depth)
        // Notional Value = Price * Quantity (Both scaled)
        // We need to unscale one of them or adjust threshold.
        // Price is 1e8, Qty is 1e8. Product is 1e16.
        // Threshold MIN_DEPTH_VALUE is 1000.0 (double).
        // Let's convert primitives to double for calculation to match logic or keep it
        // consistent.
        // Double conversion is cheap enough.

        double bidPr = bestBidPrice / 1e8;
        double bidQty = book.getBestBidQuantity() / 1e8;
        double askPr = bestAskPrice / 1e8;
        double askQty = book.getBestAskQuantity() / 1e8;

        double bidNotional = bidPr * bidQty;
        double askNotional = askPr * askQty;

        if (bidNotional < MIN_DEPTH_VALUE || askNotional < MIN_DEPTH_VALUE) {
            trip(String.format("Low liquidity for %s (Bid: %.2f, Ask: %.2f)", symbol, bidNotional, askNotional));
            return;
        }

        // 4. Check Price Deviation
        double midPrice = (bidPr + askPr) / 2.0;

        long now = System.currentTimeMillis();
        long lastUpdate = lastPriceUpdateTime.getOrDefault(symbol, 0L);
        Double lastPrice = lastValidPrices.get(symbol);

        if (lastPrice != null) {
            double deviation = Math.abs(midPrice - lastPrice) / lastPrice;
            if (deviation > MAX_PRICE_DROP_PERCENT) {
                trip(String.format("Price deviation %.2f%% for %s (Last: %.2f, Current: %.2f)",
                        deviation * 100, symbol, lastPrice, midPrice));
                return;
            }
        }

        // Update reference price periodically
        if (now - lastUpdate > PRICE_UPDATE_INTERVAL_MS) {
            lastValidPrices.put(symbol, midPrice);
            lastPriceUpdateTime.put(symbol, now);
        }
    }

    private void trip(String reason) {
        if (state.get() != State.OPEN) {
            state.set(State.OPEN);
            lastTripTime.set(System.currentTimeMillis());
            log.error("CIRCUIT BREAKER TRIPPED: {}", reason);

            // Clear reference prices on trip to avoid stuck state
            lastValidPrices.clear();
        }
    }

    public void forceReset() {
        state.set(State.CLOSED);
        lastValidPrices.clear();
        log.info("Circuit breaker manually reset");
    }
}

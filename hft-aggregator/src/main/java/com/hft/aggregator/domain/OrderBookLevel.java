package com.hft.aggregator.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a single price level in the order book.
 * Uses lock-free atomics for high-concurrency updates.
 */
@Data
@Builder
@AllArgsConstructor
public class OrderBookLevel implements Comparable<OrderBookLevel> {

    private final long priceScaled; // Price in scaled format (e.g., *1e8)
    private final AtomicLong quantityScaled; // Atomic for lock-free updates
    private final AtomicReference<String> source; // Exchange/LP ID
    private final AtomicLong lastUpdateNanos; // Nano timestamp
    private final boolean isBid; // true=bid, false=ask

    public OrderBookLevel(long priceScaled, long quantityScaled, String source, boolean isBid) {
        this.priceScaled = priceScaled;
        this.quantityScaled = new AtomicLong(quantityScaled);
        this.source = new AtomicReference<>(source);
        this.lastUpdateNanos = new AtomicLong(System.nanoTime());
        this.isBid = isBid;
    }

    /**
     * Update quantity atomically.
     */
    public void updateQuantity(long newQuantity) {
        quantityScaled.set(newQuantity);
        lastUpdateNanos.set(System.nanoTime());
    }

    /**
     * Add to quantity atomically (for same price from multiple sources).
     */
    public void addQuantity(long deltaQuantity) {
        quantityScaled.addAndGet(deltaQuantity);
        lastUpdateNanos.set(System.nanoTime());
    }

    public double getPrice() {
        return priceScaled / 1e8;
    }

    public double getQuantity() {
        return quantityScaled.get() / 1e8;
    }

    public String getSource() {
        return source.get();
    }

    public long getLastUpdateNanos() {
        return lastUpdateNanos.get();
    }

    /**
     * Check if this level is stale (older than threshold).
     */
    public boolean isStale(long maxAgeNanos) {
        return (System.nanoTime() - lastUpdateNanos.get()) > maxAgeNanos;
    }

    @Override
    public int compareTo(OrderBookLevel other) {
        if (isBid) {
            // Bids: higher price first
            return Long.compare(other.priceScaled, this.priceScaled);
        } else {
            // Asks: lower price first
            return Long.compare(this.priceScaled, other.priceScaled);
        }
    }

    @Override
    public String toString() {
        return String.format("%s @ %.8f (qty: %.8f, source: %s)",
                isBid ? "BID" : "ASK", getPrice(), getQuantity(), getSource());
    }
}

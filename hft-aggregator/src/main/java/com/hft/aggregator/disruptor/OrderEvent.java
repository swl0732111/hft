package com.hft.aggregator.disruptor;

import lombok.Data;

/**
 * Mutable event object for the Ring Buffer.
 * Pre-allocated to avoid GC.
 */
@Data
public class OrderEvent {
    private String symbol;
    private int symbolId; // Optimized ID
    private long price;
    private long quantity;
    private long timestamp;
    private String source;
    private boolean isBid;

    public void clear() {
        this.symbol = null;
        this.symbolId = -1;
        this.price = 0;
        this.quantity = 0;
        this.timestamp = 0;
        this.source = null;
        this.isBid = false;
    }
}

package com.hft.trading.event;

/**
 * Mutable trade event for Disruptor RingBuffer.
 * Pre-allocated and reused - ZERO allocations during runtime.
 */
public class TradeEvent {
    private String makerOrderId;
    private String takerOrderId;
    private String symbol;
    private long priceScaled;
    private long quantityScaled;
    private long timestamp;

    /**
     * Set trade data.
     * Called when publishing trade to RingBuffer.
     */
    public void set(String makerOrderId, String takerOrderId, String symbol,
            long priceScaled, long quantityScaled, long timestamp) {
        this.makerOrderId = makerOrderId;
        this.takerOrderId = takerOrderId;
        this.symbol = symbol;
        this.priceScaled = priceScaled;
        this.quantityScaled = quantityScaled;
        this.timestamp = timestamp;
    }

    /**
     * Clear event data for reuse.
     */
    public void clear() {
        this.makerOrderId = null;
        this.takerOrderId = null;
        this.symbol = null;
        this.priceScaled = 0;
        this.quantityScaled = 0;
        this.timestamp = 0;
    }

    // Getters and setters
    public String getMakerOrderId() {
        return makerOrderId;
    }

    public void setMakerOrderId(String makerOrderId) {
        this.makerOrderId = makerOrderId;
    }

    public String getTakerOrderId() {
        return takerOrderId;
    }

    public void setTakerOrderId(String takerOrderId) {
        this.takerOrderId = takerOrderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public long getPriceScaled() {
        return priceScaled;
    }

    public void setPriceScaled(long priceScaled) {
        this.priceScaled = priceScaled;
    }

    public long getQuantityScaled() {
        return quantityScaled;
    }

    public void setQuantityScaled(long quantityScaled) {
        this.quantityScaled = quantityScaled;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

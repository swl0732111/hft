package com.hft.trading.event;

import com.hft.trading.domain.Order;

/**
 * Mutable order event for Disruptor RingBuffer.
 * Pre-allocated and reused - ZERO allocations during runtime.
 */
public class OrderEvent {
    // Order identification
    private String orderId;
    private String accountId;
    private String walletAddress;

    // Order details
    private String symbol;
    private Order.Side side;
    private Order.Chain chain;

    // Zero-allocation pricing (scaled long)
    private long priceScaled;
    private long quantityScaled;
    private long initialQuantityScaled;

    // Metadata
    private long timestamp;
    private Order.OrderStatus status;

    // Event type
    private EventType eventType;

    public enum EventType {
        NEW_ORDER,
        CANCEL_ORDER,
        MODIFY_ORDER
    }

    /**
     * Copy data from Order object to this event.
     * Used when publishing to RingBuffer.
     */
    public void setFrom(Order order) {
        this.orderId = order.getId();
        this.accountId = order.getAccountId();
        this.walletAddress = order.getWalletAddress();
        this.symbol = order.getSymbol();
        this.side = order.getSide();
        this.chain = order.getChain();
        this.priceScaled = order.getPriceScaled();
        this.quantityScaled = order.getQuantityScaled();
        this.initialQuantityScaled = order.getInitialQuantityScaled();
        this.timestamp = order.getTimestamp();
        this.status = order.getStatus();
        this.eventType = EventType.NEW_ORDER;
    }

    /**
     * Clear event data for reuse.
     * Called after event processing.
     */
    public void clear() {
        this.orderId = null;
        this.accountId = null;
        this.walletAddress = null;
        this.symbol = null;
        this.side = null;
        this.chain = null;
        this.priceScaled = 0;
        this.quantityScaled = 0;
        this.initialQuantityScaled = 0;
        this.timestamp = 0;
        this.status = null;
        this.eventType = null;
    }

    // Getters and setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Order.Side getSide() {
        return side;
    }

    public void setSide(Order.Side side) {
        this.side = side;
    }

    public Order.Chain getChain() {
        return chain;
    }

    public void setChain(Order.Chain chain) {
        this.chain = chain;
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

    public long getInitialQuantityScaled() {
        return initialQuantityScaled;
    }

    public void setInitialQuantityScaled(long initialQuantityScaled) {
        this.initialQuantityScaled = initialQuantityScaled;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Order.OrderStatus getStatus() {
        return status;
    }

    public void setStatus(Order.OrderStatus status) {
        this.status = status;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}

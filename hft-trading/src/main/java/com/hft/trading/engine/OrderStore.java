package com.hft.trading.engine;

import com.hft.trading.domain.Order;

import java.util.List;

/**
 * Interface for order storage implementations.
 * Allows both off-heap (Chronicle Map) and in-memory (testing) implementations.
 */
public interface OrderStore {
    void putOrder(String orderId, Order order);

    Order getOrder(String orderId);

    Order removeOrder(String orderId);

    List<Order> getAllOrders();

    boolean containsOrder(String orderId);

    long size();

    void close();
}

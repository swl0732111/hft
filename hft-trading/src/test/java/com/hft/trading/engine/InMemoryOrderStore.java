package com.hft.trading.engine;

import com.hft.trading.domain.Order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple in-memory implementation of order storage for testing.
 * Avoids Chronicle Map dependency in unit tests.
 */
public class InMemoryOrderStore implements OrderStore {
    private final Map<String, Order> orders = new HashMap<>();

    public void putOrder(String orderId, Order order) {
        orders.put(orderId, order);
    }

    public Order getOrder(String orderId) {
        return orders.get(orderId);
    }

    public Order removeOrder(String orderId) {
        return orders.remove(orderId);
    }

    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }

    public boolean containsOrder(String orderId) {
        return orders.containsKey(orderId);
    }

    public long size() {
        return orders.size();
    }

    public void close() {
        orders.clear();
    }
}

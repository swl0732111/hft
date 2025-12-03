package com.hft.trading.engine;

import com.hft.trading.domain.Order;
import com.hft.common.util.FixedPointMath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.Object2ObjectHashMap;
import org.springframework.stereotype.Component;

/**
 * Handler for Iceberg orders.
 * Manages hidden quantity and automatic replenishment of display quantity.
 * Uses Agrona collections for zero-allocation performance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IcebergOrderHandler {
    private final MatchingEngine matchingEngine;

    // Track iceberg orders: orderId -> Order
    // Using Agrona for zero-allocation (no boxing, optimized for primitives)
    private final Object2ObjectHashMap<String, Order> icebergOrders = new Object2ObjectHashMap<>();

    /**
     * Process new iceberg order.
     * Adds display quantity to order book, stores hidden quantity.
     */
    public void processIcebergOrder(Order order) {
        if (order.getOrderType() != Order.OrderType.ICEBERG) {
            throw new IllegalArgumentException("Not an iceberg order");
        }

        // Validate display quantity
        if (order.getDisplayQuantityScaled() <= 0) {
            throw new IllegalArgumentException("Display quantity must be positive");
        }

        if (order.getDisplayQuantityScaled() > order.getQuantityScaled()) {
            throw new IllegalArgumentException("Display quantity cannot exceed total quantity");
        }

        // Calculate hidden quantity
        long hiddenQty = order.getQuantityScaled() - order.getDisplayQuantityScaled();
        order.setHiddenQuantityScaled(hiddenQty);

        // Store iceberg order for tracking
        icebergOrders.put(order.getId(), order);

        // Create visible order with display quantity
        Order visibleOrder = createVisibleOrder(order);

        // Add to order book
        OrderBook orderBook = matchingEngine.getOrderBook(order.getSymbol());
        orderBook.restoreOrder(visibleOrder);

        log.info("Iceberg order processed: id={}, total={}, display={}, hidden={}",
                order.getId(),
                FixedPointMath.toDouble(order.getQuantityScaled()),
                FixedPointMath.toDouble(order.getDisplayQuantityScaled()),
                FixedPointMath.toDouble(hiddenQty));
    }

    /**
     * Replenish iceberg order after partial fill.
     * Called when display quantity is fully or partially filled.
     */
    public void replenishIcebergOrder(String orderId, long filledQuantityScaled) {
        Order icebergOrder = icebergOrders.get(orderId);
        if (icebergOrder == null) {
            return; // Not an iceberg order or already completed
        }

        // Update quantities
        long remainingHidden = icebergOrder.getHiddenQuantityScaled();

        if (remainingHidden <= 0) {
            // Iceberg order fully filled
            icebergOrders.remove(orderId);
            log.info("Iceberg order completed: id={}", orderId);
            return;
        }

        // Calculate next display quantity
        long nextDisplayQty = Math.min(
                icebergOrder.getDisplayQuantityScaled(),
                remainingHidden);

        // Update hidden quantity
        icebergOrder.setHiddenQuantityScaled(remainingHidden - nextDisplayQty);

        // Create new visible order
        Order visibleOrder = createVisibleOrder(icebergOrder);
        visibleOrder.setQuantityScaled(nextDisplayQty);

        // Add to order book
        OrderBook orderBook = matchingEngine.getOrderBook(icebergOrder.getSymbol());
        orderBook.restoreOrder(visibleOrder);

        log.debug("Iceberg order replenished: id={}, display={}, hidden={}",
                orderId,
                FixedPointMath.toDouble(nextDisplayQty),
                FixedPointMath.toDouble(icebergOrder.getHiddenQuantityScaled()));
    }

    /**
     * Cancel iceberg order.
     */
    public void cancelIcebergOrder(String orderId) {
        Order icebergOrder = icebergOrders.remove(orderId);
        if (icebergOrder != null) {
            // Cancel visible portion in order book
            OrderBook orderBook = matchingEngine.getOrderBook(icebergOrder.getSymbol());
            orderBook.cancelOrder(orderId);

            log.info("Iceberg order canceled: id={}", orderId);
        }
    }

    /**
     * Check if order is an iceberg order.
     */
    public boolean isIcebergOrder(String orderId) {
        return icebergOrders.containsKey(orderId);
    }

    /**
     * Create visible order from iceberg order.
     * Only shows display quantity.
     */
    private Order createVisibleOrder(Order icebergOrder) {
        return Order.builder()
                .id(icebergOrder.getId())
                .accountId(icebergOrder.getAccountId())
                .walletAddress(icebergOrder.getWalletAddress())
                .symbol(icebergOrder.getSymbol())
                .side(icebergOrder.getSide())
                .chain(icebergOrder.getChain())
                .priceScaled(icebergOrder.getPriceScaled())
                .quantityScaled(icebergOrder.getDisplayQuantityScaled())
                .initialQuantityScaled(icebergOrder.getInitialQuantityScaled())
                .timestamp(icebergOrder.getTimestamp())
                .status(Order.OrderStatus.NEW)
                .orderType(Order.OrderType.LIMIT) // Visible as limit order
                .build();
    }
}

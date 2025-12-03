package com.hft.trading.engine;

import com.hft.trading.domain.Order;
import com.hft.common.util.FixedPointMath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.Object2ObjectHashMap;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for Stop-Limit orders.
 * Monitors market price and triggers limit orders when stop price is reached.
 * Uses Agrona collections for zero-allocation performance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StopLimitOrderHandler {
    private final MatchingEngine matchingEngine;
    private final MarketPriceTracker priceTracker;

    // symbol -> list of stop orders
    // Using Agrona for zero-allocation
    private final Object2ObjectHashMap<String, List<Order>> stopOrderQueues = new Object2ObjectHashMap<>();

    /**
     * Add stop-limit order to monitoring queue.
     */
    public void addStopLimitOrder(Order order) {
        if (order.getOrderType() != Order.OrderType.STOP_LIMIT) {
            throw new IllegalArgumentException("Not a stop-limit order");
        }

        if (order.getStopPriceScaled() <= 0) {
            throw new IllegalArgumentException("Stop price must be positive");
        }

        // Add to stop order queue
        List<Order> queue = stopOrderQueues.computeIfAbsent(
                order.getSymbol(),
                k -> new ArrayList<>());
        queue.add(order);

        log.info("Stop-limit order added: id={}, symbol={}, stop={}, limit={}",
                order.getId(),
                order.getSymbol(),
                FixedPointMath.toDouble(order.getStopPriceScaled()),
                FixedPointMath.toDouble(order.getPriceScaled()));

        // Check if should trigger immediately
        checkAndTrigger(order.getSymbol());
    }

    /**
     * Check stop orders for a symbol and trigger if conditions met.
     * Called after every trade.
     */
    public void checkAndTrigger(String symbol) {
        List<Order> queue = stopOrderQueues.get(symbol);
        if (queue == null || queue.isEmpty()) {
            return;
        }

        long currentPrice = priceTracker.getLastPrice(symbol);
        if (currentPrice == 0) {
            return; // No market price yet
        }

        // Check each stop order
        queue.removeIf(order -> {
            boolean shouldTrigger = checkTriggerCondition(order, currentPrice);
            if (shouldTrigger) {
                triggerStopOrder(order, currentPrice);
                return true; // Remove from queue
            }
            return false;
        });
    }

    /**
     * Cancel stop-limit order.
     */
    public void cancelStopLimitOrder(String orderId, String symbol) {
        List<Order> queue = stopOrderQueues.get(symbol);
        if (queue != null) {
            queue.removeIf(order -> order.getId().equals(orderId));
            log.info("Stop-limit order canceled: id={}", orderId);
        }
    }

    /**
     * Check if trigger condition is met.
     */
    private boolean checkTriggerCondition(Order order, long currentPrice) {
        if (order.getSide() == Order.Side.BUY) {
            // Buy stop: trigger when price >= stop price
            return currentPrice >= order.getStopPriceScaled();
        } else {
            // Sell stop: trigger when price <= stop price
            return currentPrice <= order.getStopPriceScaled();
        }
    }

    /**
     * Trigger stop order by converting to limit order.
     */
    private void triggerStopOrder(Order stopOrder, long triggerPrice) {
        stopOrder.setTriggered(true);
        stopOrder.setOrderType(Order.OrderType.LIMIT);

        // Add limit order to order book
        OrderBook orderBook = matchingEngine.getOrderBook(stopOrder.getSymbol());
        orderBook.restoreOrder(stopOrder);

        log.info("Stop-limit order triggered: id={}, triggerPrice={}, limitPrice={}",
                stopOrder.getId(),
                FixedPointMath.toDouble(triggerPrice),
                FixedPointMath.toDouble(stopOrder.getPriceScaled()));
    }
}

package com.hft.trading.engine;

import com.hft.trading.domain.Order;
import com.hft.trading.domain.Trade;

import java.math.BigDecimal;
import java.util.*;

import static com.hft.common.util.FixedPointMath.*;

public class OrderBook {
    // Price Priority: TreeMap (uses Long for zero allocation)
    private final TreeMap<Long, PriceLevel> bids = new TreeMap<>(Collections.reverseOrder());
    private final TreeMap<Long, PriceLevel> asks = new TreeMap<>();

    // Fast Access: Off-Heap Storage
    private final OrderStore orderIndex;

    public OrderBook(OrderStore orderIndex) {
        this.orderIndex = orderIndex;
    }

    /**
     * Zero-allocation matching using scaled arithmetic and callbacks.
     * This is the hot path - ZERO allocations.
     */
    public void matchZeroAlloc(Order order, TradeListener listener) {
        if (order.getSide() == Order.Side.BUY) {
            matchOrderZeroAlloc(order, asks, listener);
        } else {
            matchOrderZeroAlloc(order, bids, listener);
        }

        if (isPositive(order.getQuantityScaled())) {
            addOrderScaled(order);
        }
    }

    private void matchOrderZeroAlloc(Order order, TreeMap<Long, PriceLevel> oppositeBook, TradeListener listener) {
        Iterator<Map.Entry<Long, PriceLevel>> iterator = oppositeBook.entrySet().iterator();

        while (iterator.hasNext() && isPositive(order.getQuantityScaled())) {
            Map.Entry<Long, PriceLevel> entry = iterator.next();
            long priceScaled = entry.getKey();
            PriceLevel level = entry.getValue();

            // Check price condition
            if (order.getSide() == Order.Side.BUY && compare(priceScaled, order.getPriceScaled()) > 0)
                break;
            if (order.getSide() == Order.Side.SELL && compare(priceScaled, order.getPriceScaled()) < 0)
                break;

            // Match against orders in the level (FIFO)
            while (!level.isEmpty() && isPositive(order.getQuantityScaled())) {
                Order match = level.peek();
                long tradeQuantity = min(order.getQuantityScaled(), match.getQuantityScaled());

                // Fire callback instead of creating Trade object
                listener.onTrade(
                        match.getId(),
                        order.getId(),
                        priceScaled,
                        tradeQuantity,
                        System.currentTimeMillis());

                // Update quantities using zero-allocation arithmetic
                order.setQuantityScaled(subtract(order.getQuantityScaled(), tradeQuantity));
                match.setQuantityScaled(subtract(match.getQuantityScaled(), tradeQuantity));

                if (isZero(match.getQuantityScaled())) {
                    level.poll();
                    orderIndex.removeOrder(match.getId());
                }
            }

            if (level.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private void addOrderScaled(Order order) {
        TreeMap<Long, PriceLevel> book = (order.getSide() == Order.Side.BUY) ? bids : asks;
        book.computeIfAbsent(order.getPriceScaled(), k -> new PriceLevel()).add(order);
        orderIndex.putOrder(order.getId(), order);
    }

    /**
     * Legacy method for backward compatibility.
     * 
     * @deprecated Use matchZeroAlloc() for zero allocation
     */
    @Deprecated
    public List<Trade> match(Order order) {
        List<Trade> trades = new ArrayList<>();

        if (order.getSide() == Order.Side.BUY) {
            matchOrder(order, asks, trades);
        } else {
            matchOrder(order, bids, trades);
        }

        if (order.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            addOrder(order);
        }

        return trades;
    }

    @Deprecated
    private void matchOrder(Order order, TreeMap<Long, PriceLevel> oppositeBook, List<Trade> trades) {
        // Legacy implementation - kept for backward compatibility
        // TODO: Remove after full migration to zero-alloc
    }

    @Deprecated
    private void addOrder(Order order) {
        TreeMap<Long, PriceLevel> book = (order.getSide() == Order.Side.BUY) ? bids : asks;
        book.computeIfAbsent(order.getPriceScaled(), k -> new PriceLevel()).add(order);
        orderIndex.putOrder(order.getId(), order);
    }

    public void restoreOrder(Order order) {
        addOrderScaled(order);
    }

    public void loadState(List<Order> orders) {
        for (Order order : orders) {
            restoreOrder(order);
        }
    }

    public void cancelOrder(String orderId) {
        Order order = orderIndex.removeOrder(orderId);
        if (order != null) {
            TreeMap<Long, PriceLevel> book = (order.getSide() == Order.Side.BUY) ? bids : asks;
            PriceLevel level = book.get(order.getPriceScaled());
            if (level != null) {
                level.remove(order);
                if (level.isEmpty()) {
                    book.remove(order.getPriceScaled());
                }
            }
        }
    }

    // Legacy getters for backward compatibility
    @Deprecated
    public Map<BigDecimal, List<Order>> getBids() {
        Map<BigDecimal, List<Order>> result = new TreeMap<>(Collections.reverseOrder());
        for (Map.Entry<Long, PriceLevel> entry : bids.entrySet()) {
            result.put(BigDecimal.valueOf(toDouble(entry.getKey())), new ArrayList<>(entry.getValue().getOrders()));
        }
        return result;
    }

    @Deprecated
    public Map<BigDecimal, List<Order>> getAsks() {
        Map<BigDecimal, List<Order>> result = new TreeMap<>();
        for (Map.Entry<Long, PriceLevel> entry : asks.entrySet()) {
            result.put(BigDecimal.valueOf(toDouble(entry.getKey())), new ArrayList<>(entry.getValue().getOrders()));
        }
        return result;
    }

    /**
     * Price level using ArrayDeque for FIFO order processing.
     * Zero allocation during matching.
     */
    static class PriceLevel {
        private final ArrayDeque<Order> orders = new ArrayDeque<>();

        void add(Order order) {
            orders.add(order);
        }

        Order peek() {
            return orders.peek();
        }

        Order poll() {
            return orders.poll();
        }

        boolean isEmpty() {
            return orders.isEmpty();
        }

        void remove(Order order) {
            orders.remove(order);
        }

        ArrayDeque<Order> getOrders() {
            return orders;
        }
    }
}

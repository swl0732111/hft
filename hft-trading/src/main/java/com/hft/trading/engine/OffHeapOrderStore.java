package com.hft.trading.engine;

import com.hft.trading.domain.Order;
import net.openhft.chronicle.map.ChronicleMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OffHeapOrderStore implements OrderStore, AutoCloseable {
    private final ChronicleMap<String, Order> orderMap;

    public OffHeapOrderStore(String filePath, long maxEntries) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();

        this.orderMap = ChronicleMap
                .of(String.class, Order.class)
                .name("order-store")
                .entries(maxEntries)
                .averageKey("ORDER-12345678-1234-1234-1234-123456789012")
                .averageValue(createAverageOrder())
                .createPersistedTo(file);
    }

    private Order createAverageOrder() {
        return Order.builder()
                .id("ORDER-12345678-1234-1234-1234-123456789012")
                .accountId("ACCT-12345678-1234-1234-1234-123456789012")
                .walletAddress("0x1234567890123456789012345678901234567890")
                .symbol("BTC-USDC")
                .side(Order.Side.BUY)
                .chain(Order.Chain.ETHEREUM)
                .price(java.math.BigDecimal.valueOf(50000.00))
                .quantity(java.math.BigDecimal.valueOf(1.0))
                .initialQuantity(java.math.BigDecimal.valueOf(1.0))
                .timestamp(System.currentTimeMillis())
                .status(Order.OrderStatus.NEW)
                .build();
    }

    public void putOrder(String orderId, Order order) {
        orderMap.put(orderId, order);
    }

    public Order getOrder(String orderId) {
        return orderMap.get(orderId);
    }

    public Order removeOrder(String orderId) {
        return orderMap.remove(orderId);
    }

    public List<Order> getAllOrders() {
        return new ArrayList<>(orderMap.values());
    }

    public boolean containsOrder(String orderId) {
        return orderMap.containsKey(orderId);
    }

    public long size() {
        return orderMap.size();
    }

    @Override
    public void close() {
        orderMap.close();
    }
}

package com.hft.trading.engine;

import com.hft.trading.domain.Order;
import com.hft.trading.domain.Trade;
import com.hft.trading.fix.MarketDataPublisher;
import com.hft.trading.repository.OrderRepository;
import com.hft.trading.service.TransactionLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MatchingEngine {
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();
    private final OrderRepository orderRepository;
    private final MarketDataPublisher marketDataPublisher;
    private final TransactionLogService transactionLogService;
    private final OffHeapOrderStore offHeapOrderStore;

    public List<Trade> processOrder(Order order) {
        OrderBook orderBook = getOrderBook(order.getSymbol());
        synchronized (orderBook) {
            // Log order placement
            transactionLogService.logOrderPlaced(order);

            List<Trade> trades = orderBook.match(order);

            // Update taker order status
            updateOrderStatus(order);
            orderRepository.save(order);

            // Log trades and filled orders
            for (Trade trade : trades) {
                transactionLogService.logOrderMatched(trade);
                marketDataPublisher.publishTrade(trade);
            }

            if (order.getStatus() == Order.OrderStatus.FILLED) {
                transactionLogService.logOrderFilled(order);
            }

            return trades;
        }
    }

    public OrderBook getOrderBook(String symbol) {
        return orderBooks.computeIfAbsent(symbol, k -> {
            OrderBook book = new OrderBook(offHeapOrderStore);
            // Restore from DB
            List<Order> openOrders = orderRepository.findBySymbolAndQuantityGreaterThan(symbol, BigDecimal.ZERO);
            book.loadState(openOrders);
            return book;
        });
    }

    public void cancelOrder(String orderId, String symbol) {
        OrderBook orderBook = getOrderBook(symbol);
        synchronized (orderBook) {
            orderBook.cancelOrder(orderId);
            transactionLogService.logOrderCanceled(orderId, symbol);
        }
    }

    private void updateOrderStatus(Order order) {
        if (order.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(Order.OrderStatus.FILLED);
        } else if (order.getQuantity().compareTo(order.getInitialQuantity()) < 0) {
            order.setStatus(Order.OrderStatus.PARTIALLY_FILLED);
        }
    }
}

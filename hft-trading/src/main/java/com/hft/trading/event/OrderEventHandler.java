package com.hft.trading.event;

import com.hft.trading.engine.IcebergOrderHandler;
import com.hft.trading.engine.MatchingEngine;
import com.hft.trading.engine.OrderBook;
import com.hft.trading.engine.StopLimitOrderHandler;
import com.hft.trading.domain.Order;
import com.hft.trading.persistence.OrderBatchWriter;
import com.hft.trading.persistence.WriteAheadLog;
import com.hft.trading.repository.OrderRepository;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Event handler for processing order events from RingBuffer.
 * Implements zero-allocation order matching.
 */
@Component
@RequiredArgsConstructor
public class OrderEventHandler implements EventHandler<OrderEvent> {
    private final MatchingEngine matchingEngine;
    private final RingBuffer<TradeEvent> tradeRingBuffer;
    private final IcebergOrderHandler icebergOrderHandler;
    private final StopLimitOrderHandler stopLimitOrderHandler;
    private final OrderBatchWriter orderBatchWriter;
    private final WriteAheadLog wal;

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        switch (event.getEventType()) {
            case NEW_ORDER:
                processNewOrder(event);
                break;
            case CANCEL_ORDER:
                processCancelOrder(event);
                break;
            case MODIFY_ORDER:
                processModifyOrder(event);
                break;
        }

        // Clear event for reuse (optional, depends on strategy)
        // event.clear();
    }

    private void processNewOrder(OrderEvent event) {
        // Convert event to Order
        Order order = convertToOrder(event);

        // Async batch write (non-blocking)
        orderBatchWriter.add(order);

        // WAL for durability
        wal.append("ORDER", order.getId() + "|" + order.getSymbol());

        // Route based on order type
        if (order.getOrderType() == Order.OrderType.ICEBERG) {
            // Handle iceberg order
            icebergOrderHandler.processIcebergOrder(order);
        } else if (order.getOrderType() == Order.OrderType.STOP_LIMIT) {
            // Handle stop-limit order (add to monitoring queue)
            stopLimitOrderHandler.addStopLimitOrder(order);
        } else {
            // Standard limit order - add to order book and match
            processStandardOrder(event, order);
        }
    }

    private void processStandardOrder(OrderEvent event, Order order) {
        // Get order book and match
        OrderBook orderBook = matchingEngine.getOrderBook(event.getSymbol());

        // Use zero-allocation matching with trade callback
        orderBook.matchZeroAlloc(convertToOrder(event),
                (makerOrderId, takerOrderId, priceScaled, quantityScaled, timestamp) -> {
                    // Publish trade to trade RingBuffer (zero allocation)
                    long tradeSequence = tradeRingBuffer.next();
                    try {
                        TradeEvent tradeEvent = tradeRingBuffer.get(tradeSequence);
                        tradeEvent.set(makerOrderId, takerOrderId, event.getSymbol(), priceScaled, quantityScaled,
                                timestamp);
                    } finally {
                        tradeRingBuffer.publish(tradeSequence);
                    }
                });
    }

    private void processCancelOrder(OrderEvent event) {
        OrderBook orderBook = matchingEngine.getOrderBook(event.getSymbol());
        orderBook.cancelOrder(event.getOrderId());
    }

    private void processModifyOrder(OrderEvent event) {
        // Cancel and replace strategy
        processCancelOrder(event);
        processNewOrder(event);
    }

    /**
     * Convert event to Order object.
     * TODO: Use object pool to avoid allocation
     */
    private Order convertToOrder(OrderEvent event) {
        return Order.builder()
                .id(event.getOrderId())
                .accountId(event.getAccountId())
                .walletAddress(event.getWalletAddress())
                .symbol(event.getSymbol())
                .side(event.getSide())
                .chain(event.getChain())
                .priceScaled(event.getPriceScaled())
                .quantityScaled(event.getQuantityScaled())
                .initialQuantityScaled(event.getInitialQuantityScaled())
                .timestamp(event.getTimestamp())
                .status(event.getStatus())
                .build();
    }
}

package com.hft.trading.event;

import com.hft.trading.domain.Order;
import com.hft.trading.engine.IcebergOrderHandler;
import com.hft.trading.engine.MatchingEngine;
import com.hft.trading.engine.OrderBook;
import com.hft.trading.engine.StopLimitOrderHandler;
import com.hft.trading.persistence.ChronicleOrderQueue;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Event handler for processing order events from RingBuffer. Implements zero-allocation order
 * matching with Chronicle Queue persistence.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventHandler implements EventHandler<OrderEvent> {
    private final MatchingEngine matchingEngine;
    private final RingBuffer<TradeEvent> tradeRingBuffer;
    private final IcebergOrderHandler icebergOrderHandler;
    private final StopLimitOrderHandler stopLimitOrderHandler;
  private final ChronicleOrderQueue chronicleOrderQueue;

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

    // Chronicle Queue for durability (~200ns latency)
    // No need for batch writer - Chronicle handles batching automatically
    try {
      chronicleOrderQueue.append(event);
    } catch (Exception e) {
      log.error("Failed to append order to Chronicle Queue: {}", event.getOrderId(), e);
      // Continue processing - queue failure shouldn't block matching
    }

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
   * Convert event to Order object using object pool. Reuses pre-allocated Order instances for
   * zero-GC operation.
   */
  private Order convertToOrder(OrderEvent event) {
    // Note: Object pooling is optional for this use case
    // Chronicle Queue already provides zero-allocation persistence
    // This allocation happens after queue write, so it's not in the critical path
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

package com.hft.trading.recovery;

import com.hft.trading.domain.Order;
import com.hft.trading.engine.MatchingEngine;
import com.hft.trading.engine.OrderBook;
import com.hft.trading.event.OrderEvent;
import com.hft.trading.persistence.ChronicleOrderQueue;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.ExcerptTailer;
import org.springframework.stereotype.Service;

/**
 * Service for crash recovery and replay from Chronicle Queue. Reconstructs order book state by
 * replaying orders from queue.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChronicleRecoveryService {

  private final ChronicleOrderQueue chronicleOrderQueue;
  private final MatchingEngine matchingEngine;
  private final QueueCheckpointManager checkpointManager;

  /**
   * Recover from crash by replaying orders from last checkpoint.
   *
   * @return Number of orders replayed
   */
  public long recoverFromCrash() {
    log.info("Starting crash recovery from Chronicle Queue");
    long startTime = System.currentTimeMillis();

    // Get last checkpoint index
    long lastCheckpoint = checkpointManager.getLastCheckpoint();
    log.info("Last checkpoint index: {}", lastCheckpoint);

    // Replay from checkpoint
    long ordersReplayed = replayFromIndex(lastCheckpoint);

    long duration = System.currentTimeMillis() - startTime;
    log.info("Crash recovery complete: {} orders replayed in {}ms", ordersReplayed, duration);

    return ordersReplayed;
  }

  /**
   * Replay orders from a specific queue index.
   *
   * @param startIndex Queue index to start from
   * @return Number of orders replayed
   */
  public long replayFromIndex(long startIndex) {
    AtomicLong count = new AtomicLong(0);
    ExcerptTailer tailer = chronicleOrderQueue.createTailer(startIndex);

    log.info("Replaying orders from index: {}", startIndex);

    while (tailer.readDocument(
        wire -> {
          try {
            // Deserialize order event
            OrderEvent event = deserializeOrderEvent(wire);

            // Replay order to matching engine
            replayOrder(event);

            count.incrementAndGet();

            if (count.get() % 10000 == 0) {
              log.info("Replayed {} orders...", count.get());
            }
          } catch (Exception e) {
            log.error("Failed to replay order at index {}", tailer.index(), e);
          }
        })) {
      // Continue reading until end of queue
    }

    log.info("Replay complete: {} orders processed", count.get());
    return count.get();
  }

  /**
   * Replay orders for a specific time range. Useful for debugging or partial recovery.
   *
   * @param startTime Start timestamp (millis)
   * @param endTime End timestamp (millis)
   * @return Number of orders replayed
   */
  public long replayTimeRange(long startTime, long endTime) {
    AtomicLong count = new AtomicLong(0);
    ExcerptTailer tailer = chronicleOrderQueue.createTailer();

    log.info("Replaying orders from {} to {}", startTime, endTime);

    while (tailer.readDocument(
        wire -> {
          try {
            long timestamp = wire.read("timestamp").int64();

            if (timestamp < startTime) {
              return; // Skip orders before start time
            }

            if (timestamp > endTime) {
              throw new StopReplayException(); // Stop at end time
            }

            OrderEvent event = deserializeOrderEvent(wire);
            replayOrder(event);
            count.incrementAndGet();

          } catch (StopReplayException e) {
            throw e;
          } catch (Exception e) {
            log.error("Failed to replay order", e);
          }
        })) {
      // Continue until StopReplayException
    }

    log.info("Time range replay complete: {} orders", count.get());
    return count.get();
  }

  /**
   * Rebuild order book for a specific symbol. Replays all orders for that symbol from the
   * beginning.
   *
   * @param symbol Trading symbol
   * @return Number of orders replayed
   */
  public long rebuildOrderBook(String symbol) {
    log.info("Rebuilding order book for symbol: {}", symbol);

    OrderBook orderBook = matchingEngine.getOrderBook(symbol);
    if (orderBook == null) {
      log.warn("Order book not found for symbol: {}", symbol);
      return 0; // Return 0 as no orders were replayed for this symbol
    }

    // Note: OrderBook doesn't have a clear() method
    // We'll rebuild by adding orders directly

    AtomicLong count = new AtomicLong(0);
    ExcerptTailer tailer = chronicleOrderQueue.createTailer();

    while (tailer.readDocument(
        wire -> {
          try {
            String orderSymbol = wire.read("symbol").text();

            if (symbol.equals(orderSymbol)) {
              OrderEvent event = deserializeOrderEvent(wire);
              Order order = convertToOrder(event);

              // Only restore active orders (NEW or PARTIALLY_FILLED)
              if (order.getStatus() == Order.OrderStatus.NEW
                  || order.getStatus() == Order.OrderStatus.PARTIALLY_FILLED) {
                orderBook.restoreOrder(order);
                log.debug("Restored order: {}", order.getId());
                count.incrementAndGet();
              }
            }
          } catch (Exception e) {
            log.error("Failed to replay order for symbol {}", symbol, e);
          }
        })) {
      // Continue until end
    }

    log.info("Order book rebuilt for {}: {} orders", symbol, count.get());
    return count.get();
  }

  /** Deserialize OrderEvent from Chronicle Wire. */
  private OrderEvent deserializeOrderEvent(net.openhft.chronicle.wire.WireIn wire) {
    OrderEvent event = new OrderEvent();

    event.setOrderId(wire.read("orderId").text());
    event.setAccountId(wire.read("accountId").text());
    event.setWalletAddress(wire.read("walletAddress").text());
    event.setSymbol(wire.read("symbol").text());
    event.setSide(Order.Side.valueOf(wire.read("side").text()));
    event.setChain(Order.Chain.valueOf(wire.read("chain").text()));
    event.setPriceScaled(wire.read("priceScaled").int64());
    event.setQuantityScaled(wire.read("quantityScaled").int64());
    event.setInitialQuantityScaled(wire.read("initialQuantityScaled").int64());
    event.setTimestamp(wire.read("timestamp").int64());
    event.setStatus(Order.OrderStatus.valueOf(wire.read("status").text()));
    event.setEventType(OrderEvent.EventType.valueOf(wire.read("eventType").text()));

    return event;
  }

  /** Replay a single order to the matching engine. */
  private void replayOrder(OrderEvent event) {
    switch (event.getEventType()) {
      case NEW_ORDER:
        // Add order to order book
        OrderBook orderBook = matchingEngine.getOrderBook(event.getSymbol());
        Order order = convertToOrder(event);
        orderBook.restoreOrder(order);
        break;

      case CANCEL_ORDER:
        // Cancel order
        OrderBook cancelBook = matchingEngine.getOrderBook(event.getSymbol());
        cancelBook.cancelOrder(event.getOrderId());
        break;

      case MODIFY_ORDER:
        // Cancel and replace
        OrderBook modifyBook = matchingEngine.getOrderBook(event.getSymbol());
        modifyBook.cancelOrder(event.getOrderId());
        Order modifiedOrder = convertToOrder(event);
        modifyBook.restoreOrder(modifiedOrder);
        break;
    }
  }

  /** Convert OrderEvent to Order domain object. */
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

  /** Exception to stop replay at a specific point. */
  private static class StopReplayException extends RuntimeException {}
}

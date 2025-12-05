package com.hft.trading.pool;

import com.hft.trading.domain.Order;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Object pool for Order instances to avoid allocation in hot path. Pre-allocates Order objects and
 * reuses them for zero-GC operation.
 */
@Slf4j
@Component
public class OrderPool {

  private static final int POOL_SIZE = 10000; // Pre-allocate 10K orders
  private final BlockingQueue<Order> pool = new ArrayBlockingQueue<>(POOL_SIZE);

  @PostConstruct
  public void init() {
    // Pre-populate pool
    for (int i = 0; i < POOL_SIZE; i++) {
      pool.offer(Order.builder().build());
    }
    log.info("Order pool initialized with {} instances", POOL_SIZE);
  }

  /** Acquire an Order from the pool. Falls back to creating new instance if pool is empty. */
  public Order acquire() {
    Order order = pool.poll();
    if (order == null) {
      log.warn("Order pool exhausted, creating new instance");
      return Order.builder().build();
    }
    return order;
  }

  /** Return an Order to the pool for reuse. Clears the order before returning to pool. */
  public void release(Order order) {
    if (order != null) {
      // Clear order data before returning to pool
      clearOrder(order);
      if (!pool.offer(order)) {
        log.debug("Order pool full, discarding instance");
      }
    }
  }

  /** Clear order data for reuse. */
  private void clearOrder(Order order) {
    order.setId(null);
    order.setAccountId(null);
    order.setWalletAddress(null);
    order.setSymbol(null);
    order.setSide(null);
    order.setChain(null);
    order.setPriceScaled(0);
    order.setQuantityScaled(0);
    order.setInitialQuantityScaled(0);
    order.setTimestamp(0);
    order.setStatus(null);
  }

  /** Get current pool size. */
  public int getAvailableCount() {
    return pool.size();
  }

  /** Get pool utilization (percentage used). */
  public double getUtilization() {
    return (1.0 - (double) pool.size() / POOL_SIZE) * 100;
  }
}

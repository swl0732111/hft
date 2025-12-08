package com.hft.trading.persistence;

import com.hft.trading.domain.Order;
import com.hft.trading.domain.Trade;
import com.hft.trading.recovery.QueueCheckpointManager;
import com.hft.trading.repository.OrderRepository;
import com.hft.trading.repository.TradeRepository;
import com.hft.trading.repository.TransactionLogRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.queue.ExcerptTailer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background service to persist Chronicle Queue data to database. Tails the
 * order queue
 * asynchronously and batch-writes to DB. This keeps the hot path (queue writes)
 * completely separate
 * from DB writes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChronicleToDbWriter {

  private static final int BATCH_SIZE = 1000;
  private static final int CHECKPOINT_INTERVAL = 10000; // Checkpoint every 10K orders
  private static final int MAX_RETRIES = 3;
  private static final long INITIAL_RETRY_DELAY_MS = 100;
  private final ChronicleOrderQueue chronicleOrderQueue;
  private final OrderRepository orderRepository;
  private final QueueCheckpointManager checkpointManager;
  private final Object tailerLock = new Object();
  // Dead letter queue for failed writes
  private final List<Order> deadLetterQueue = new ArrayList<>();
  private ExcerptTailer tailer;
  private volatile boolean running = true;
  private Thread writerThread;

  @PostConstruct
  public void init() {
    log.info("ChronicleToDbWriter initializing...");

    // Start background thread
    writerThread = new Thread(this::processQueue, "chronicle-db-writer");
    writerThread.setDaemon(true);
    writerThread.start();
  }

  /** Background thread to tail queue and write to DB. */
  private void processQueue() {
    // Create tailer in this thread to avoid cross-thread access issues
    long lastCheckpoint = checkpointManager.getLastCheckpoint();
    synchronized (tailerLock) {
      tailer = chronicleOrderQueue.createTailer(lastCheckpoint);
    }
    log.info("Chronicle-to-DB writer thread started from checkpoint: {}", lastCheckpoint);

    List<Order> batch = new ArrayList<>(BATCH_SIZE);
    long processedCount = 0;

    while (running) {
      try {
        // Read from queue
        boolean hasData;
        synchronized (tailerLock) {
          hasData = tailer.readDocument(
              wire -> {
                try {
                  Order order = deserializeOrder(wire);
                  batch.add(order);
                } catch (Exception e) {
                  log.error("Failed to deserialize order", e);
                }
              });
        }

        // Batch write to DB
        if (batch.size() >= BATCH_SIZE || (!hasData && !batch.isEmpty())) {
          writeBatchToDb(batch);
          processedCount += batch.size();
          batch.clear();

          // Checkpoint periodically
          if (processedCount % CHECKPOINT_INTERVAL == 0) {
            long currentIndex;
            synchronized (tailerLock) {
              currentIndex = tailer.index();
            }
            checkpointManager.saveCheckpoint(currentIndex, 0);
            log.info("Checkpoint saved at index: {}", currentIndex);
          }
        }

        // Sleep if no data
        if (!hasData) {
          Thread.sleep(10); // 10ms sleep when queue is empty
        }

      } catch (InterruptedException e) {
        log.info("Chronicle-to-DB writer interrupted");
        break;
      } catch (ClosedIllegalStateException e) {
        log.info("Chronicle queue closed, stopping writer thread");
        break;
      } catch (Exception e) {
        log.error("Error in Chronicle-to-DB writer", e);
        try {
          Thread.sleep(1000); // Back off on error
        } catch (InterruptedException ie) {
          break;
        }
      }
    }

    // Final flush
    if (!batch.isEmpty()) {
      writeBatchToDb(batch);
    }

    log.info("Chronicle-to-DB writer thread stopped");
  }

  /** Write batch of orders to database with retry logic. */
  private void writeBatchToDb(List<Order> orders) {
    int retries = 0;
    long retryDelay = INITIAL_RETRY_DELAY_MS;

    while (retries <= MAX_RETRIES) {
      try {
        orderRepository.saveAll(orders);
        log.debug("Wrote {} orders to database", orders.size());
        return; // Success

      } catch (Exception e) {
        retries++;

        if (retries > MAX_RETRIES) {
          log.error(
              "Failed to write batch after {} retries, moving to dead letter queue",
              MAX_RETRIES,
              e);

          // Add to dead letter queue for manual intervention
          synchronized (deadLetterQueue) {
            deadLetterQueue.addAll(orders);
            log.warn("Dead letter queue size: {}", deadLetterQueue.size());
          }
          return;
        }

        // Exponential backoff
        log.warn(
            "Database write failed (attempt {}/{}), retrying in {}ms",
            retries,
            MAX_RETRIES,
            retryDelay,
            e);

        try {
          Thread.sleep(retryDelay);
          retryDelay *= 2; // Exponential backoff
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

  /** Deserialize Order from Chronicle Wire. */
  private Order deserializeOrder(net.openhft.chronicle.wire.WireIn wire) {
    return Order.builder()
        .id(wire.read("orderId").text())
        .accountId(wire.read("accountId").text())
        .walletAddress(wire.read("walletAddress").text())
        .symbol(wire.read("symbol").text())
        .side(Order.Side.valueOf(wire.read("side").text()))
        .chain(Order.Chain.valueOf(wire.read("chain").text()))
        .priceScaled(wire.read("priceScaled").int64())
        .quantityScaled(wire.read("quantityScaled").int64())
        .initialQuantityScaled(wire.read("initialQuantityScaled").int64())
        .timestamp(wire.read("timestamp").int64())
        .status(Order.OrderStatus.valueOf(wire.read("status").text()))
        .build();
  }

  /** Get current lag (how far behind the writer is). */
  public long getLag() {
    long lastWritten;
    synchronized (tailerLock) {
      lastWritten = tailer.index();
    }
    long lastAppended = chronicleOrderQueue.lastIndex();
    return lastAppended - lastWritten;
  }

  /** Scheduled health check. */
  @Scheduled(fixedRate = 60000) // Every minute
  public void healthCheck() {
    long lag = getLag();
    if (lag > 100000) {
      log.warn("Chronicle-to-DB writer is lagging: {} orders behind", lag);
    } else {
      log.info("Chronicle-to-DB writer lag: {} orders", lag);
    }
  }

  /** Shutdown hook. */
  @PreDestroy
  public void shutdown() {
    running = false;
    log.info("Chronicle-to-DB writer shutting down");

    if (writerThread != null) {
      try {
        writerThread.join(2000); // Wait up to 2 seconds for thread to finish
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Interrupted while waiting for writer thread to stop");
      }
    }

    // Log dead letter queue status
    synchronized (deadLetterQueue) {
      if (!deadLetterQueue.isEmpty()) {
        log.error("Shutdown with {} orders in dead letter queue", deadLetterQueue.size());
        // TODO: Persist dead letter queue to disk for recovery
      }
    }
  }

  /** Get dead letter queue size. */
  public int getDeadLetterQueueSize() {
    synchronized (deadLetterQueue) {
      return deadLetterQueue.size();
    }
  }

  /** Retry dead letter queue items. Can be called manually or scheduled. */
  public void retryDeadLetterQueue() {
    List<Order> itemsToRetry;
    synchronized (deadLetterQueue) {
      if (deadLetterQueue.isEmpty()) {
        return;
      }
      itemsToRetry = new ArrayList<>(deadLetterQueue);
      deadLetterQueue.clear();
    }

    log.info("Retrying {} orders from dead letter queue", itemsToRetry.size());
    writeBatchToDb(itemsToRetry);
  }
}

package com.hft.trading.recovery;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Manages checkpoints for Chronicle Queue recovery. Stores the last successfully processed queue
 * index for crash recovery.
 */
@Slf4j
@Component
public class QueueCheckpointManager {

  private final AtomicLong lastCheckpoint = new AtomicLong(0);
  @Qualifier("checkpointQueue")
  @Autowired
  private ChronicleQueue checkpointQueue;
  private ExcerptAppender appender;

  @PostConstruct
  public void init() {
    appender = checkpointQueue.createAppender();
    loadLastCheckpoint();
    log.info("Checkpoint manager initialized: last checkpoint = {}", lastCheckpoint.get());
  }

  /**
   * Save a checkpoint (last processed queue index).
   *
   * @param orderQueueIndex Last processed order queue index
   * @param tradeQueueIndex Last processed trade queue index
   */
  public void saveCheckpoint(long orderQueueIndex, long tradeQueueIndex) {
    appender.writeDocument(
        wire -> {
          wire.write("timestamp").int64(System.currentTimeMillis());
          wire.write("orderQueueIndex").int64(orderQueueIndex);
          wire.write("tradeQueueIndex").int64(tradeQueueIndex);
        });

    lastCheckpoint.set(orderQueueIndex);
    log.debug("Checkpoint saved: order={}, trade={}", orderQueueIndex, tradeQueueIndex);
  }

  /**
   * Get the last checkpoint index.
   *
   * @return Last checkpoint index, or 0 if no checkpoint exists
   */
  public long getLastCheckpoint() {
    return lastCheckpoint.get();
  }

  /** Load last checkpoint from queue. */
  private void loadLastCheckpoint() {
    ExcerptTailer tailer = checkpointQueue.createTailer().toEnd();

    // Read the last checkpoint
    try {
      // The original code reads the last document and sets lastCheckpoint.
      // The instruction snippet introduces 'checkpoint' and 'deserializeCheckpoint'.
      // To make the code syntactically correct and functional with existing
      // structure,
      // I will adapt the snippet to use the existing 'lastCheckpoint' AtomicLong.
      // If the user intends a full refactor to a Checkpoint object, that would be a
      // larger change.
      if (tailer.readDocument(
          wire -> {
            long orderIndex = wire.read("orderQueueIndex").int64();
            long tradeIndex = wire.read("tradeQueueIndex").int64();
            lastCheckpoint.set(orderIndex); // Update the existing AtomicLong
            log.info(
                "Loaded last checkpoint: orderIndex={}, tradeIndex={}", orderIndex, tradeIndex);
          })) {
        // Checkpoint loaded successfully
      } else {
        log.info("No checkpoint found, starting from beginning");
        lastCheckpoint.set(0); // Ensure it's 0 if no checkpoint is found
      }
    } catch (Exception e) {
      log.error("Failed to load last checkpoint", e);
      lastCheckpoint.set(0); // Reset in case of error
    }
  }

  /**
   * Get checkpoint history (last N checkpoints).
   *
   * <p>NOTE: Disabled due to Chronicle Queue API limitations. ExcerptTailer doesn't have
   * moveToPreviousIndex() method.
   *
   * @param count Number of checkpoints to retrieve
   * @return Array of checkpoint indices
   */
  /*
   * public long[] getCheckpointHistory(int count) {
   * long[] history = new long[count];
   * int index = 0;
   *
   * ExcerptTailer tailer = checkpointQueue.createTailer().toEnd();
   *
   * // Move back N entries
   * for (int i = 0; i < count; i++) {
   * if (!tailer.moveToPreviousIndex()) {
   * break;
   * }
   * }
   *
   * // Read forward
   * while (index < count && tailer.readDocument(wire -> {
   * history[index] = wire.read("orderQueueIndex").int64();
   * })) {
   * index++;
   * }
   *
   * return history;
   * }
   */
}

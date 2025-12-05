package com.hft.trading.config;

import java.io.File;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Chronicle Queue instances. Provides separate queues for orders and trades with
 * optimized settings.
 */
@Configuration
public class ChronicleQueueConfig {

  @Value("${chronicle.queue.base-dir:${user.home}/hft-data/chronicle}")
  private String baseDir;

  @Value("${chronicle.queue.roll-cycle:HOURLY}")
  private String rollCycle;

  @Value("${chronicle.queue.buffer-capacity:256}")
  private int bufferCapacity; // KB

  /**
   * Order queue - stores all order events for recovery. Roll cycle: HOURLY (creates new file every
   * hour) Buffer: 256KB (tuned for order size)
   */
  @Bean(name = "orderQueue")
  public ChronicleQueue orderQueue() {
    String orderQueuePath = baseDir + "/orders";
    ensureDirectoryExists(orderQueuePath);

    return SingleChronicleQueueBuilder.builder()
        .path(orderQueuePath)
        .rollCycle(getRollCycle())
        .blockSize(bufferCapacity * 1024)
        .build();
  }

  /**
   * Trade queue - stores all trade events for recovery. Separate from orders for better isolation
   * and performance.
   */
  @Bean(name = "tradeQueue")
  public ChronicleQueue tradeQueue() {
    String tradeQueuePath = baseDir + "/trades";
    ensureDirectoryExists(tradeQueuePath);

    return SingleChronicleQueueBuilder.builder()
        .path(tradeQueuePath)
        .rollCycle(getRollCycle())
        .blockSize(bufferCapacity * 1024)
        .build();
  }

  /** Checkpoint queue - stores recovery checkpoints. Smaller, less frequent writes. */
  @Bean(name = "checkpointQueue")
  public ChronicleQueue checkpointQueue() {
    String checkpointQueuePath = baseDir + "/checkpoints";
    ensureDirectoryExists(checkpointQueuePath);

    return SingleChronicleQueueBuilder.builder()
        .path(checkpointQueuePath)
        .rollCycle(RollCycles.FAST_DAILY) // Less frequent rollover
        .blockSize(64 * 1024) // Smaller buffer
        .build();
  }

  private RollCycles getRollCycle() {
    switch (rollCycle.toUpperCase()) {
      case "HOURLY":
        return RollCycles.FAST_HOURLY;
      case "DAILY":
        return RollCycles.FAST_DAILY;
      default:
        return RollCycles.FAST_HOURLY;
    }
  }

  private void ensureDirectoryExists(String path) {
    File dir = new File(path);
    if (!dir.exists()) {
      dir.mkdirs();
    }
  }
}

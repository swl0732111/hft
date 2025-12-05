package com.hft.trading.benchmark;

import com.hft.trading.domain.Order;
import com.hft.trading.event.OrderEvent;
import com.hft.trading.persistence.ChronicleOrderQueue;
import com.hft.trading.persistence.WriteAheadLog;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Performance benchmark comparing Chronicle Queue vs old WAL. Run with: mvn test
 * -Dtest=ChronicleQueueBenchmark
 */
@Slf4j
@SpringBootTest
public class ChronicleQueueBenchmark {

  private static final int WARMUP_ITERATIONS = 10000;
  private static final int BENCHMARK_ITERATIONS = 100000;
  @Autowired(required = false)
  private ChronicleOrderQueue chronicleQueue;
  @Autowired(required = false)
  private WriteAheadLog oldWal;

  @Test
  public void testChronicleQueueWriteLatency() {
    if (chronicleQueue == null) {
      log.warn("Chronicle Queue not available, skipping benchmark");
      return;
    }

    log.info("=== Chronicle Queue Write Latency Benchmark ===");

    // Warmup
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      OrderEvent event = createTestOrderEvent();
      chronicleQueue.append(event);
    }

    // Benchmark
    long[] latencies = new long[BENCHMARK_ITERATIONS];
    for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
      OrderEvent event = createTestOrderEvent();

      long start = System.nanoTime();
      chronicleQueue.append(event);
      long end = System.nanoTime();

      latencies[i] = end - start;
    }

    printLatencyStats("Chronicle Queue", latencies);
  }

  @Test
  public void testOldWalWriteLatency() {
    if (oldWal == null) {
      log.warn("Old WAL not available, skipping benchmark");
      return;
    }

    log.info("=== Old WAL Write Latency Benchmark ===");

    // Warmup
    for (int i = 0; i < WARMUP_ITERATIONS / 10; i++) { // Fewer warmup for slow WAL
      oldWal.append("ORDER", UUID.randomUUID().toString());
    }

    // Benchmark
    long[] latencies = new long[BENCHMARK_ITERATIONS / 10]; // Fewer iterations
    for (int i = 0; i < latencies.length; i++) {
      long start = System.nanoTime();
      oldWal.append("ORDER", UUID.randomUUID().toString());
      long end = System.nanoTime();

      latencies[i] = end - start;
    }

    printLatencyStats("Old WAL", latencies);
  }

  @Test
  public void testChronicleQueueThroughput() {
    if (chronicleQueue == null) {
      log.warn("Chronicle Queue not available, skipping benchmark");
      return;
    }

    log.info("=== Chronicle Queue Throughput Benchmark ===");

    int iterations = 1000000; // 1 million
    long start = System.currentTimeMillis();

    for (int i = 0; i < iterations; i++) {
      OrderEvent event = createTestOrderEvent();
      chronicleQueue.append(event);
    }

    long end = System.currentTimeMillis();
    long duration = end - start;
    double throughput = (double) iterations / duration * 1000;

    log.info(
        "Chronicle Queue Throughput: {:.2f} ops/sec ({} ops in {}ms)",
        throughput,
        iterations,
        duration);
  }

  private OrderEvent createTestOrderEvent() {
    OrderEvent event = new OrderEvent();
    event.setOrderId(UUID.randomUUID().toString());
    event.setAccountId("test-account");
    event.setWalletAddress("0x1234567890abcdef");
    event.setSymbol("BTC-USDC");
    event.setSide(Order.Side.BUY);
    event.setChain(Order.Chain.ETHEREUM);
    event.setPriceScaled(50000_00000000L);
    event.setQuantityScaled(1_00000000L);
    event.setInitialQuantityScaled(1_00000000L);
    event.setTimestamp(System.currentTimeMillis());
    event.setStatus(Order.OrderStatus.NEW);
    event.setEventType(OrderEvent.EventType.NEW_ORDER);
    return event;
  }

  private void printLatencyStats(String name, long[] latencies) {
    java.util.Arrays.sort(latencies);

    long p50 = latencies[latencies.length / 2];
    long p95 = latencies[(int) (latencies.length * 0.95)];
    long p99 = latencies[(int) (latencies.length * 0.99)];
    long p999 = latencies[(int) (latencies.length * 0.999)];
    long max = latencies[latencies.length - 1];

    double avg = java.util.Arrays.stream(latencies).average().orElse(0);

    log.info("{} Latency Statistics:", name);
    log.info("  Average: {:.2f} ns ({:.2f} μs)", avg, avg / 1000);
    log.info("  p50:     {} ns ({:.2f} μs)", p50, p50 / 1000.0);
    log.info("  p95:     {} ns ({:.2f} μs)", p95, p95 / 1000.0);
    log.info("  p99:     {} ns ({:.2f} μs)", p99, p99 / 1000.0);
    log.info("  p999:    {} ns ({:.2f} μs)", p999, p999 / 1000.0);
    log.info("  Max:     {} ns ({:.2f} μs)", max, max / 1000.0);
  }
}

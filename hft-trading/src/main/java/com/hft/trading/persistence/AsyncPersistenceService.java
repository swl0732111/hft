package com.hft.trading.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Async persistence service coordinating batch writes.
 * Flushes buffers periodically to ensure timely persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncPersistenceService {
    private final OrderBatchWriter orderBatchWriter;
    private final TradeBatchWriter tradeBatchWriter;

    private static final long FLUSH_INTERVAL_MS = 10;

    @PostConstruct
    public void init() {
        log.info("AsyncPersistenceService initialized with flush interval: {}ms",
                FLUSH_INTERVAL_MS);
    }

    /**
     * Periodic flush every 10ms.
     * Ensures data is persisted even if batch size not reached.
     */
    @Scheduled(fixedRate = FLUSH_INTERVAL_MS)
    public void periodicFlush() {
        try {
            int ordersPending = orderBatchWriter.getBufferSize();
            int tradesPending = tradeBatchWriter.getBufferSize();

            if (ordersPending > 0 || tradesPending > 0) {
                long startTime = System.nanoTime();

                orderBatchWriter.flush();
                tradeBatchWriter.flush();

                long duration = System.nanoTime() - startTime;

                log.trace("Periodic flush: {} orders, {} trades in {}μs",
                        ordersPending, tradesPending, duration / 1000);
            }
        } catch (Exception e) {
            log.error("Periodic flush failed", e);
        }
    }

    /**
     * Graceful shutdown - flush all pending data.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down AsyncPersistenceService - flushing buffers");

        try {
            orderBatchWriter.flush();
            tradeBatchWriter.flush();

            log.info("Final flush completed: {} orders, {} trades written",
                    orderBatchWriter.getTotalWritten(),
                    tradeBatchWriter.getTotalWritten());
        } catch (Exception e) {
            log.error("Shutdown flush failed", e);
        }
    }

    /**
     * Get metrics.
     */
    public String getMetrics() {
        return String.format(
                "Orders: %d buffered, %d total | Trades: %d buffered, %d total",
                orderBatchWriter.getBufferSize(),
                orderBatchWriter.getTotalWritten(),
                tradeBatchWriter.getBufferSize(),
                tradeBatchWriter.getTotalWritten());
    }
}

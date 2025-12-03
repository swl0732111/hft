package com.hft.trading.persistence;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Generic batch writer for high-throughput persistence.
 * Accumulates items and flushes in batches to minimize DB round-trips.
 * 
 * @param <T>  Entity type
 * @param <ID> Entity ID type
 */
@Slf4j
public class BatchWriter<T, ID> {
    private final CrudRepository<T, ID> repository;
    private final int batchSize;
    private final List<T> buffer;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile long lastFlushTime = System.currentTimeMillis();
    private volatile int totalWritten = 0;

    public BatchWriter(CrudRepository<T, ID> repository, int batchSize) {
        this.repository = repository;
        this.batchSize = batchSize;
        this.buffer = new ArrayList<>(batchSize);
    }

    /**
     * Add item to batch buffer.
     * Flushes automatically when batch size reached.
     */
    public void add(T item) {
        lock.lock();
        try {
            buffer.add(item);

            if (buffer.size() >= batchSize) {
                flushInternal();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Force flush all pending items.
     * Thread-safe.
     */
    public void flush() {
        lock.lock();
        try {
            if (!buffer.isEmpty()) {
                flushInternal();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Internal flush implementation.
     * Must be called with lock held.
     */
    private void flushInternal() {
        if (buffer.isEmpty()) {
            return;
        }

        long startTime = System.nanoTime();
        int count = buffer.size();

        try {
            // Batch insert
            repository.saveAll(buffer);

            long duration = System.nanoTime() - startTime;
            totalWritten += count;
            lastFlushTime = System.currentTimeMillis();

            log.debug("Batch write completed: {} items in {}μs",
                    count, duration / 1000);

        } catch (Exception e) {
            log.error("Batch write failed: {} items", count, e);
            throw new RuntimeException("Batch write failed", e);
        } finally {
            buffer.clear();
        }
    }

    /**
     * Get current buffer size.
     */
    public int getBufferSize() {
        lock.lock();
        try {
            return buffer.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get total items written.
     */
    public int getTotalWritten() {
        return totalWritten;
    }

    /**
     * Get time since last flush (ms).
     */
    public long getTimeSinceLastFlush() {
        return System.currentTimeMillis() - lastFlushTime;
    }
}

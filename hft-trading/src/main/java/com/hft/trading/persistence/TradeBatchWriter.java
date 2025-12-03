package com.hft.trading.persistence;

import com.hft.trading.domain.TransactionLog;
import com.hft.trading.repository.TransactionLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Specialized batch writer for Trade/TransactionLog entities.
 * Optimized for very high-throughput trade persistence.
 */
@Slf4j
@Component
public class TradeBatchWriter {
    private final TransactionLogRepository transactionLogRepository;
    private final BatchWriter<TransactionLog, String> batchWriter;

    private static final int BATCH_SIZE = 200; // Higher for trades

    public TradeBatchWriter(TransactionLogRepository transactionLogRepository) {
        this.transactionLogRepository = transactionLogRepository;
        this.batchWriter = new BatchWriter<>(transactionLogRepository, BATCH_SIZE);
    }

    @PostConstruct
    public void init() {
        log.info("TradeBatchWriter initialized with batch size: {}", BATCH_SIZE);
    }

    /**
     * Add trade to batch buffer.
     */
    public void add(TransactionLog trade) {
        batchWriter.add(trade);
    }

    /**
     * Force flush pending trades.
     */
    public void flush() {
        batchWriter.flush();
    }

    /**
     * Get metrics.
     */
    public int getBufferSize() {
        return batchWriter.getBufferSize();
    }

    public int getTotalWritten() {
        return batchWriter.getTotalWritten();
    }
}

package com.hft.trading.persistence;

import com.hft.trading.domain.Order;
import com.hft.trading.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Specialized batch writer for Order entities.
 * Optimized for high-throughput order persistence.
 */
@Slf4j
@Component
public class OrderBatchWriter {
    private final OrderRepository orderRepository;
    private final BatchWriter<Order, String> batchWriter;

    private static final int BATCH_SIZE = 100;

    public OrderBatchWriter(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
        this.batchWriter = new BatchWriter<>(orderRepository, BATCH_SIZE);
    }

    @PostConstruct
    public void init() {
        log.info("OrderBatchWriter initialized with batch size: {}", BATCH_SIZE);
    }

    /**
     * Add order to batch buffer.
     */
    public void add(Order order) {
        batchWriter.add(order);
    }

    /**
     * Force flush pending orders.
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

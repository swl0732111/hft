package com.hft.trading.persistence;

import com.hft.trading.event.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Chronicle Queue implementation for order events.
 * Provides nanosecond-latency persistence with zero-GC operation.
 */
@Slf4j
@Component
public class ChronicleOrderQueue {

    @Qualifier("orderQueue")
    @Autowired
    private ChronicleQueue queue;

    private ExcerptAppender appender;

    @PostConstruct
    public void init() {
        appender = queue.createAppender();
        log.info("Chronicle Order Queue initialized: {}", queue.file().getAbsolutePath());
    }

    /**
     * Append order event to queue.
     * Zero-allocation, ~200ns latency.
     * 
     * @param event Order event to persist
     */
    public void append(OrderEvent event) {
        appender.writeDocument(wire -> {
            wire.write("orderId").text(event.getOrderId());
            wire.write("accountId").text(event.getAccountId());
            wire.write("walletAddress").text(event.getWalletAddress());
            wire.write("symbol").text(event.getSymbol());
            wire.write("side").text(event.getSide().name());
            wire.write("chain").text(event.getChain().name());
            wire.write("priceScaled").int64(event.getPriceScaled());
            wire.write("quantityScaled").int64(event.getQuantityScaled());
            wire.write("initialQuantityScaled").int64(event.getInitialQuantityScaled());
            wire.write("timestamp").int64(event.getTimestamp());
            wire.write("status").text(event.getStatus().name());
            wire.write("eventType").text(event.getEventType().name());
        });
    }

    /**
     * Create a tailer (reader) for recovery/replay.
     * 
     * @return Tailer positioned at the start of the queue
     */
    public ExcerptTailer createTailer() {
        return queue.createTailer();
    }

    /**
     * Create a tailer starting from a specific index.
     * 
     * @param index Queue index to start from
     * @return Tailer positioned at the specified index
     */
    public ExcerptTailer createTailer(long index) {
        ExcerptTailer tailer = queue.createTailer();
        tailer.moveToIndex(index);
        return tailer;
    }

    /**
     * Get last index in queue.
     */
    public long lastIndex() {
        try {
            return appender.lastIndexAppended();
        } catch (IllegalStateException e) {
            // Queue is empty, return -1
            return -1;
        }
    }

    /**
     * Get queue file path.
     */
    public String getPath() {
        return queue.file().getAbsolutePath();
    }

    /**
     * Get queue size in bytes.
     */
    public long getSize() {
        return queue.file().length();
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (queue != null) {
                queue.close();
            }
            log.info("Chronicle Order Queue shutdown complete");
        } catch (Exception e) {
            log.error("Chronicle Order Queue shutdown failed", e);
        }
    }
}

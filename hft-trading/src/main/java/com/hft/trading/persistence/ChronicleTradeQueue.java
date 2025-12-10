package com.hft.trading.persistence;

import com.hft.trading.event.TradeEvent;
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
 * Chronicle Queue implementation for trade events.
 * Separate queue from orders for better isolation and performance.
 */
@Slf4j
@Component
public class ChronicleTradeQueue {

    @Qualifier("tradeQueue")
    @Autowired
    private ChronicleQueue queue;

    private ExcerptAppender appender;

    @PostConstruct
    public void init() {
        appender = queue.createAppender();
        log.info("Chronicle Trade Queue initialized: {}", queue.file().getAbsolutePath());
    }

    /**
     * Append trade event to queue.
     * Zero-allocation, ~200ns latency.
     * 
     * @param event Trade event to persist
     */
    public void append(TradeEvent event) {
        appender.writeDocument(wire -> {
            wire.write("makerOrderId").text(event.getMakerOrderId());
            wire.write("takerOrderId").text(event.getTakerOrderId());
            wire.write("symbol").text(event.getSymbol());
            wire.write("priceScaled").int64(event.getPriceScaled());
            wire.write("quantityScaled").int64(event.getQuantityScaled());
            wire.write("timestamp").int64(event.getTimestamp());
        });
    }

    /**
     * Create a tailer (reader) for recovery/replay.
     */
    public ExcerptTailer createTailer() {
        return queue.createTailer();
    }

    /**
     * Create a tailer starting from a specific index.
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
            log.info("Chronicle Trade Queue shutdown complete");
        } catch (Exception e) {
            log.error("Chronicle Trade Queue shutdown failed", e);
        }
    }
}

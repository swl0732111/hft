// package com.hft.trading.monitoring;
//
// import com.hft.trading.persistence.ChronicleOrderQueue;
// import com.hft.trading.persistence.ChronicleToDbWriter;
// import com.hft.trading.persistence.ChronicleTradeQueue;
// import io.micrometer.core.instrument.MeterRegistry;
// import io.micrometer.core.instrument.Timer;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Component;
//
// import jakarta.annotation.PostConstruct;
// import java.util.concurrent.TimeUnit;
//
/// **
// * Metrics and monitoring for Chronicle Queue.
// * Exposes metrics via Micrometer for Prometheus/Grafana.
// */
// @Slf4j
// @Component
// @RequiredArgsConstructor
// public class ChronicleQueueMetrics {
//
//    private final ChronicleOrderQueue orderQueue;
//    private final ChronicleTradeQueue tradeQueue;
//    private final ChronicleToDbWriter dbWriter;
//    @Autowired(required = false)
//    private MeterRegistry meterRegistry;
//
//    private Timer orderWriteTimer;
//    private Timer tradeWriteTimer;
//
//    @PostConstruct
//    public void init() {
//        if (meterRegistry == null) {
//            log.warn("MeterRegistry not available, Chronicle Queue metrics disabled");
//            return;
//        }
//
//        if (orderQueue == null || tradeQueue == null) {
//            log.warn("Chronicle Queues not available, metrics disabled");
//            return;
//        }
//
//        // Register timers for write latency
//        orderWriteTimer = Timer.builder("chronicle.order.write.latency")
//                .description("Order write latency to Chronicle Queue")
//                .tag("queue", "orders")
//                .publishPercentiles(0.5, 0.95, 0.99, 0.999)
//                .register(meterRegistry);
//
//        tradeWriteTimer = Timer.builder("chronicle.trade.write.latency")
//                .description("Trade write latency to Chronicle Queue")
//                .tag("queue", "trades")
//                .publishPercentiles(0.5, 0.95, 0.99, 0.999)
//                .register(meterRegistry);
//
//        // Register gauges for queue size
//        meterRegistry.gauge("chronicle.order.queue.size", orderQueue, q -> q.getSize());
//        meterRegistry.gauge("chronicle.trade.queue.size", tradeQueue, q -> q.getSize());
//
//        // Register gauge for DB writer lag
//        meterRegistry.gauge("chronicle.db.writer.lag", dbWriter, w -> w.getLag());
//
//        log.info("Chronicle Queue metrics initialized");
//    }
//
//    /**
//     * Record order write latency.
//     */
//    public void recordOrderWrite(long nanos) {
//        if (orderWriteTimer != null) { // Added null check for existing method
//            orderWriteTimer.record(nanos, TimeUnit.NANOSECONDS);
//        }
//    }
//
//    /**
//     * Record trade write latency.
//     */
//    public void recordTradeWrite(long nanos) {
//        if (tradeWriteTimer != null) { // Added null check for existing method
//            tradeWriteTimer.record(nanos, TimeUnit.NANOSECONDS);
//        }
//    }
//
//    /**
//     * Scheduled metrics collection.
//     */
//    @Scheduled(fixedRate = 10000) // Every 10 seconds
//    public void collectMetrics() {
//        long orderQueueSize = orderQueue.getSize();
//        long tradeQueueSize = tradeQueue.getSize();
//        long dbLag = dbWriter.getLag();
//
//        log.info("Chronicle Queue Metrics - Order queue: {} bytes, Trade queue: {} bytes, DB lag:
// {} orders",
//                orderQueueSize, tradeQueueSize, dbLag);
//
//        // Alert if lag is too high
//        if (dbLag > 100000) {
//            log.warn("DB writer lag is high: {} orders", dbLag);
//        }
//    }
//
//    /**
//     * Get current metrics snapshot.
//     */
//    public MetricsSnapshot getSnapshot() {
//        return MetricsSnapshot.builder()
//                .orderQueueSize(orderQueue.getSize())
//                .tradeQueueSize(tradeQueue.getSize())
//                .orderQueueLastIndex(orderQueue.lastIndex())
//                .tradeQueueLastIndex(tradeQueue.lastIndex())
//                .dbWriterLag(dbWriter.getLag())
//                .build();
//    }
//
//    @lombok.Data
//    @lombok.Builder
//    public static class MetricsSnapshot {
//        private long orderQueueSize;
//        private long tradeQueueSize;
//        private long orderQueueLastIndex;
//        private long tradeQueueLastIndex;
//        private long dbWriterLag;
//    }
// }

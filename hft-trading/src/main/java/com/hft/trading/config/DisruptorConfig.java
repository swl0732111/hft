package com.hft.trading.config;

import com.hft.trading.event.OrderEvent;
import com.hft.trading.event.OrderEventHandler;
import com.hft.trading.event.TradeEvent;
import com.hft.trading.event.TradeEventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.lmax.disruptor.BusySpinWaitStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Disruptor configuration for zero-allocation event processing.
 * 
 * RingBuffer sizes MUST be powers of 2 for optimal performance.
 * WaitStrategy determines latency vs CPU trade-off.
 */
@Configuration
public class DisruptorConfig {

    // RingBuffer sizes (powers of 2)
    private static final int ORDER_BUFFER_SIZE = 8192; // 8K orders
    private static final int TRADE_BUFFER_SIZE = 16384; // 16K trades

    /**
     * Configure trade RingBuffer.
     * Must be created before order RingBuffer (dependency).
     */
    @Bean
    public Disruptor<TradeEvent> tradeDisruptor(TradeEventHandler tradeEventHandler) {
        // Create Disruptor with BusySpinWaitStrategy for lowest latency
        Disruptor<TradeEvent> disruptor = new Disruptor<>(
                TradeEvent::new,
                TRADE_BUFFER_SIZE,
                DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI, // Multiple producers (order handlers)
                new BusySpinWaitStrategy() // Lowest latency, highest CPU
        );

        // Wire event handler
        disruptor.handleEventsWith(tradeEventHandler);

        // Start the disruptor
        disruptor.start();

        return disruptor;
    }

    /**
     * Expose trade RingBuffer for publishing.
     */
    @Bean
    public RingBuffer<TradeEvent> tradeRingBuffer(Disruptor<TradeEvent> tradeDisruptor) {
        return tradeDisruptor.getRingBuffer();
    }

    /**
     * Configure order RingBuffer.
     */
    @Bean
    public Disruptor<OrderEvent> orderDisruptor(
            OrderEventHandler orderEventHandler,
            RingBuffer<TradeEvent> tradeRingBuffer) {

        // Create Disruptor with BusySpinWaitStrategy for lowest latency
        Disruptor<OrderEvent> disruptor = new Disruptor<>(
                OrderEvent::new,
                ORDER_BUFFER_SIZE,
                DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI, // Multiple API threads
                new BusySpinWaitStrategy() // Lowest latency, highest CPU
        );

        // Wire event handler
        disruptor.handleEventsWith(orderEventHandler);

        // Start the disruptor
        disruptor.start();

        return disruptor;
    }

    /**
     * Expose order RingBuffer for publishing.
     */
    @Bean
    public RingBuffer<OrderEvent> orderRingBuffer(Disruptor<OrderEvent> orderDisruptor) {
        return orderDisruptor.getRingBuffer();
    }

    /**
     * Shutdown hook to gracefully stop Disruptors.
     */
    @Bean
    public DisruptorShutdownHook disruptorShutdownHook(
            Disruptor<OrderEvent> orderDisruptor,
            Disruptor<TradeEvent> tradeDisruptor) {
        return new DisruptorShutdownHook(orderDisruptor, tradeDisruptor);
    }

    /**
     * Graceful shutdown for Disruptors.
     */
    public static class DisruptorShutdownHook {
        private final Disruptor<OrderEvent> orderDisruptor;
        private final Disruptor<TradeEvent> tradeDisruptor;

        public DisruptorShutdownHook(Disruptor<OrderEvent> orderDisruptor,
                Disruptor<TradeEvent> tradeDisruptor) {
            this.orderDisruptor = orderDisruptor;
            this.tradeDisruptor = tradeDisruptor;

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                orderDisruptor.shutdown();
                tradeDisruptor.shutdown();
            }));
        }
    }
}

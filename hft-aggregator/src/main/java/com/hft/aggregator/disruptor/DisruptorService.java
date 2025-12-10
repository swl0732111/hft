package com.hft.aggregator.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadFactory;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisruptorService {

    private final OrderEventHandler orderEventHandler;
    private Disruptor<OrderEvent> disruptor;
    private RingBuffer<OrderEvent> ringBuffer;

    // Power of 2 buffer size - Increased to 1M for high throughput
    private static final int BUFFER_size = 1024 * 1024;

    @PostConstruct
    public void start() {
        ThreadFactory threadFactory = DaemonThreadFactory.INSTANCE;
        // Optimization: Use YieldingWaitStrategy or BusySpinWaitStrategy for HFT
        // Yielding is a good balance between throughput and CPU. BusySpin is max
        // throughput but 100% CPU.
        disruptor = new Disruptor<>(
                new OrderEventFactory(),
                BUFFER_size,
                threadFactory,
                com.lmax.disruptor.dsl.ProducerType.MULTI,
                new com.lmax.disruptor.YieldingWaitStrategy());

        disruptor.handleEventsWith(orderEventHandler);
        disruptor.start();

        ringBuffer = disruptor.getRingBuffer();
        log.info("Disruptor started with buffer size: {} and YieldingWaitStrategy", BUFFER_size);
    }

    @PreDestroy
    public void stop() {
        if (disruptor != null) {
            disruptor.shutdown();
            log.info("Disruptor stopped");
        }
    }

    public void publish(String symbol, long price, long quantity, String source, boolean isBid) {
        // Resolve ID outside the lock (or inside if creating) - SymbolDictionary is
        // fast
        int symbolId = com.hft.aggregator.domain.SymbolDictionary.getOrCreateId(symbol);

        long sequence = ringBuffer.next();
        try {
            OrderEvent event = ringBuffer.get(sequence);
            event.setSymbol(symbol);
            event.setSymbolId(symbolId);
            event.setPrice(price);
            event.setQuantity(quantity);
            event.setSource(source);
            event.setBid(isBid);
            event.setTimestamp(System.currentTimeMillis());
        } finally {
            ringBuffer.publish(sequence);
        }
    }
}

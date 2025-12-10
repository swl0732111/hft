package com.hft.aggregator.benchmark;

import com.hft.aggregator.aggregator.OrderBookAggregator;
import com.hft.aggregator.disruptor.DisruptorService;
import com.hft.aggregator.disruptor.OrderEventHandler;
import com.hft.aggregator.edgecase.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderBookBenchmark {

    private static final Logger log = LoggerFactory.getLogger(OrderBookBenchmark.class);
    private static final String SYMBOL = "BTC/USDT";
    private static final String SOURCE = "coinbase";

    @Test
    public void runDisruptorBenchmark() {
        CircuitBreaker circuitBreaker = new CircuitBreaker();
        OrderBookAggregator aggregator = new OrderBookAggregator(circuitBreaker);
        OrderEventHandler handler = new OrderEventHandler(aggregator);
        DisruptorService disruptorService = new DisruptorService(handler);

        disruptorService.start();

        // Warmup
        log.info("Starting Disruptor Warmup...");
        runLoop(disruptorService, 100_000);

        try {
            Thread.sleep(1000); // Give consumer time to catch up
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Measurement
        log.info("Starting Disruptor Measurement...");
        long start = System.nanoTime();
        int iterations = 1_000_000;
        runLoop(disruptorService, iterations);
        long end = System.nanoTime();

        // Note: This measures producer throughput (writing to RingBuffer).
        // Actual E2E depends on consumer speed, but Disruptor handles backpressure
        // efficiently.

        long durationNs = end - start;
        double durationMs = durationNs / 1_000_000.0;
        double opsPerSec = (iterations * 2) / (durationNs / 1_000_000_000.0); // *2 for bid + ask

        log.info("Finished {} iterations in {} ms", iterations, String.format("%.2f", durationMs));
        log.info("Producer Throughput: {} ops/sec", String.format("%.2f", opsPerSec));
        log.info("Latency per op (Producer): {} ns", String.format("%.2f", (double) durationNs / (iterations * 2)));

        disruptorService.stop();
    }

    private void runLoop(DisruptorService service, int iterations) {
        for (int i = 0; i < iterations; i++) {
            long price = 50000_00000000L + (i % 100) * 100_000000L;
            long qty = 1_00000000L;
            service.publish(SYMBOL, price, qty, SOURCE, true); // Bid
            service.publish(SYMBOL, price + 100_000000L, qty, SOURCE, false); // Ask
        }
    }
}

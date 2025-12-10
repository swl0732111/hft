package com.hft.aggregator.benchmark;

import com.hft.aggregator.domain.ArrayOrderBook;
import org.junit.jupiter.api.Test;

public class ArrayOrderBookBenchmark {

    @Test
    public void benchmarkUpdateBid() {
        int depth = 100;
        int iterations = 10_000_000;
        ArrayOrderBook book = new ArrayOrderBook(1, depth);

        long start = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            // Simulate random updates within a price range
            long price = 50000_00000000L + (i % 50) * 100_000000L; // Scaled price
            long quantity = 100_000000L;
            book.updateBid(price, quantity, 1);
        }

        long end = System.nanoTime();
        long duration = end - start;

        System.out.printf("ArrayOrderBook Updates: %d ops in %.2f ms%n", iterations, duration / 1_000_000.0);
        System.out.printf("Throughput: %.2f ops/sec%n", iterations / (duration / 1_000_000_000.0));
        System.out.printf("Latency per op: %.2f ns%n", (double) duration / iterations);
    }
}

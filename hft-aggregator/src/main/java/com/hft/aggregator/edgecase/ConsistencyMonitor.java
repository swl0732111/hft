package com.hft.aggregator.edgecase;

import com.hft.aggregator.aggregator.OrderBookAggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Periodically monitors data consistency across aggregated books.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsistencyMonitor {

    private final OrderBookAggregator aggregator;
    private final CircuitBreaker circuitBreaker;

    @Scheduled(fixedRate = 1000) // Run every second
    public void monitor() {
        aggregator.getAllBooks().forEach(this::checkBook);
    }

    /**
     * Explicit check method called by higher level components or scheduled tasks if
     * we expose the books.
     */
    public void checkBook(com.hft.aggregator.domain.ArrayOrderBook book) {
        if (book.getSpread() < 0) {
            // Need to resolve symbol for logging
            String symbol = com.hft.aggregator.domain.SymbolDictionary.getSymbol(book.getSymbolId());
            log.error("Consistency Error: Negative spread for {}", symbol);
            circuitBreaker.onFailure("Negative spread detected by monitor");
        }

        // Check for stale data beyond normal pruning
        long now = System.nanoTime();
        if (now - book.getLastUpdateNanos() > 30_000_000_000L) { // 30 seconds
            String symbol = com.hft.aggregator.domain.SymbolDictionary.getSymbol(book.getSymbolId());
            log.warn("Stale order book for {}. Last update > 30s ago", symbol);
            // Could trip breaker or alert
        }
    }
}

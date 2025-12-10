package com.hft.aggregator.aggregator;

import com.hft.aggregator.domain.ArrayOrderBook;
import com.hft.aggregator.domain.SymbolDictionary;
import com.hft.aggregator.edgecase.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Core order book aggregation engine.
 * Maintains array-backed order books for high-performance zero-allocation
 * updates.
 */
@Slf4j
@Component
public class OrderBookAggregator {

    // Using AtomicReferenceArray for thread-safe publication of new books if we
    // added dynamic symbols
    // But ArrayOrderBook itself is single-writer.
    // For max speed, we could use a simple array if we guarantee registration
    // happens before processing.
    // Using AtomicReferenceArray for safety.
    private final AtomicReferenceArray<ArrayOrderBook> books;
    private final CircuitBreaker circuitBreaker;
    private final int maxDepth;
    private final long staleThresholdNanos;

    // Sampling counter for Circuit Breaker
    private long updateCount = 0;

    public OrderBookAggregator(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
        this.maxDepth = 20;
        this.staleThresholdNanos = 5_000_000_000L; // 5 seconds
        // Initialize for MAX_SYMBOLS
        this.books = new AtomicReferenceArray<>(4096);
    }

    /**
     * Get or create aggregated order book for a symbol ID.
     */
    public ArrayOrderBook getOrderBook(int symbolId) {
        ArrayOrderBook book = books.get(symbolId);
        if (book == null) {
            book = new ArrayOrderBook(symbolId, maxDepth);
            if (!books.compareAndSet(symbolId, null, book)) {
                book = books.get(symbolId); // Lost race, get winner
            }
        }
        return book;
    }

    // Legacy support for String-based APIs (should be minimized)
    public ArrayOrderBook getOrderBook(String symbol) {
        int id = SymbolDictionary.getOrCreateId(symbol);
        return getOrderBook(id);
    }

    /**
     * Update bid level using symbol ID.
     * Zero Allocation path.
     */
    public void updateBid(int symbolId, long priceScaled, long quantityScaled, String source) {
        ArrayOrderBook book = getOrderBook(symbolId);
        int sourceId = SymbolDictionary.getOrCreateId(source);
        book.updateBid(priceScaled, quantityScaled, sourceId);

        // Sampling Circuit Breaker
        if ((++updateCount & 1023) == 0) {
            // Adapting checkMarketConditions for ArrayOrderBook is needed,
            // or we skip specific CB logic for this optimized path for now
            // and implement a lightweight check.
            circuitBreaker.checkMarketConditions(book);
        }
    }

    /**
     * Update ask level using symbol ID.
     * Zero Allocation path.
     */
    public void updateAsk(int symbolId, long priceScaled, long quantityScaled, String source) {
        ArrayOrderBook book = getOrderBook(symbolId);
        int sourceId = SymbolDictionary.getOrCreateId(source);
        book.updateAsk(priceScaled, quantityScaled, sourceId);

        if ((++updateCount & 1023) == 0) {
            circuitBreaker.checkMarketConditions(book);
        }
    }

    /**
     * Get all managed order books (for monitoring).
     */
    public java.util.List<ArrayOrderBook> getAllBooks() {
        java.util.List<ArrayOrderBook> activeBooks = new java.util.ArrayList<>();
        int length = books.length();
        for (int i = 0; i < length; i++) {
            ArrayOrderBook book = books.get(i);
            if (book != null) {
                activeBooks.add(book);
            }
        }
        return activeBooks;
    }

    // Public API for fetching snapshot (still returns DTOs or can expose array
    // view)
    // For now, minimal support to keep compilation active.
}

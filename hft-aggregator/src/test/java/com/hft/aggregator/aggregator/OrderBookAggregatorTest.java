package com.hft.aggregator.aggregator;

import com.hft.aggregator.domain.ArrayOrderBook;
import com.hft.aggregator.domain.SymbolDictionary;
import com.hft.aggregator.edgecase.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderBookAggregatorTest {

    private OrderBookAggregator aggregator;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = mock(CircuitBreaker.class);
        aggregator = new OrderBookAggregator(circuitBreaker);
    }

    @Test
    void testUpdateBidUpdatesBookAndChecksCircuit() {
        int symbolId = SymbolDictionary.getOrCreateId("BTC/USDT");

        // Circuit Breaker is sampled every 1024 updates
        // We need to trigger it.
        // updateCount starts at 0. ++updateCount.
        // (x & 1023) == 0 means x must be multiple of 1024.

        for (int i = 0; i < 1024; i++) {
            aggregator.updateBid(symbolId, 50000_00000000L, 1_00000000L, "binance");
        }

        ArrayOrderBook book = aggregator.getOrderBook(symbolId);
        assertNotNull(book);
        assertTrue(book.getBidCount() > 0);
        assertEquals(50000_00000000L, book.getBestBidPrice());

        verify(circuitBreaker, atLeastOnce()).checkMarketConditions(book);
    }
}

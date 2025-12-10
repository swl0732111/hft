package com.hft.aggregator.edgecase;

import com.hft.aggregator.domain.ArrayOrderBook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = new CircuitBreaker();
    }

    @Test
    void testInitialState() {
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState().get());
        assertTrue(circuitBreaker.allowRequest());
    }

    @Test
    void testManualTrip() {
        circuitBreaker.onFailure("Manual trip");
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState().get());
        assertFalse(circuitBreaker.allowRequest());
    }

    @Test
    void testCrossedMarketTripsBreaker() {
        ArrayOrderBook book = new ArrayOrderBook(1, 20);
        int sourceId = 1;
        // Bid 100 > Ask 90
        book.updateBid(100_00000000L, 1_00000000L, sourceId);
        book.updateAsk(90_00000000L, 1_00000000L, sourceId);

        circuitBreaker.checkMarketConditions(book);

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState().get());
    }

    @Test
    void testEmptyBookTripsBreaker() {
        ArrayOrderBook book = new ArrayOrderBook(1, 20);
        // Empty by default

        circuitBreaker.checkMarketConditions(book);
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState().get());
    }

    @Test
    void testNormalConditions() {
        // Use a real object
        ArrayOrderBook realBook = new ArrayOrderBook(1, 20);
        int sourceId = 1;

        // Use proper scaled values: Price 50000, Qty 1.0 -> Notional 50000 > 1000
        // threshold
        realBook.updateBid(50000_00000000L, 1_00000000L, sourceId);
        realBook.updateAsk(50010_00000000L, 1_00000000L, sourceId);

        circuitBreaker.checkMarketConditions(realBook);
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState().get());
    }

    @Test
    void testPriceDeviationTripsBreaker() throws InterruptedException {
        ArrayOrderBook book = new ArrayOrderBook(1, 20);
        int sourceId = 1;

        // Initial price: 50000
        book.updateBid(50000_00000000L, 100_00000000L, sourceId); // High liquidity
        book.updateAsk(50010_00000000L, 100_00000000L, sourceId);

        circuitBreaker.checkMarketConditions(book); // Set reference price

        // Crash price: 40000 (20% drop)
        // Clear old levels for simplicity (ArrayOrderBook holds levels, need to
        // overwrite or clear)
        book.updateBid(40000_00000000L, 100_00000000L, sourceId);
        book.updateAsk(40010_00000000L, 100_00000000L, sourceId);
        // Note: ArrayOrderBook keeps old levels unless overwritten or removed.
        // 50000 is still there if not removed.
        // But Best Bid will range.
        // If we add 40000, and 50000 is there, Best Bid is still 50000!
        // So we must remove 50000.
        book.updateBid(50000_00000000L, 0, sourceId); // Remove
        book.updateAsk(50010_00000000L, 0, sourceId); // Remove

        circuitBreaker.checkMarketConditions(book);
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState().get());
    }

    @Test
    void testLowLiquidityTripsBreaker() {
        ArrayOrderBook book = new ArrayOrderBook(1, 20);
        int sourceId = 1;

        // Price 50000 but Quantity Tiny (0.00001) -> Notional = 0.5 < 1000
        book.updateBid(50000_00000000L, 1000L, sourceId);
        book.updateAsk(50010_00000000L, 100000000L, sourceId);

        circuitBreaker.checkMarketConditions(book);
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState().get());
    }

    @Test
    void testForceReset() {
        circuitBreaker.onFailure("test");
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState().get());

        circuitBreaker.forceReset();
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState().get());
    }
}

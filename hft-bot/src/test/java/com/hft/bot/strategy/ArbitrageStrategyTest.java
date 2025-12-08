package com.hft.bot.strategy;

import com.hft.trading.domain.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ArbitrageStrategyTest {

    private ArbitrageStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new ArbitrageStrategy();
    }

    @Test
    void checkOpportunity_ShouldReturnOrder_WhenSpreadIsPositive() {
        Optional<Order> result = strategy.checkOpportunity("BTC-USDC", 50000.0, 50020.0);

        assertTrue(result.isPresent());
        Order order = result.get();
        assertEquals("BTC-USDC", order.getSymbol());
        assertEquals(Order.Side.BUY, order.getSide());
        assertEquals(50000.0, order.getPriceAsDouble());
    }

    @Test
    void checkOpportunity_ShouldReturnEmpty_WhenSpreadIsLow() {
        Optional<Order> result = strategy.checkOpportunity("BTC-USDC", 50000.0, 50005.0);

        assertFalse(result.isPresent());
    }

    @Test
    void checkOpportunity_ShouldReturnEmpty_WhenPriceAIsHigher() {
        Optional<Order> result = strategy.checkOpportunity("BTC-USDC", 50020.0, 50000.0);

        assertFalse(result.isPresent());
    }
}

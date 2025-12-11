package com.hft.trading.service.risk;

import com.hft.trading.domain.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderRateLimitRuleTest {

    private OrderRateLimitRule rule;

    @BeforeEach
    void setUp() {
        rule = new OrderRateLimitRule();
    }

    @Test
    void shouldAllowOrdersWithSufficientInterval() throws InterruptedException {
        Order order = Order.builder().accountId("1").build();

        rule.validate(order);

        // Wait for retail interval (100ms)
        Thread.sleep(110);

        assertDoesNotThrow(() -> rule.validate(order));
    }

    @Test
    void shouldRejectSpammingOrders() {
        Order order = Order.builder().accountId("1").build();

        rule.validate(order);

        // Immediate second order should fail
        assertThrows(IllegalArgumentException.class, () -> rule.validate(order));
    }

    @Test
    void shouldAllowMarketMakerHighFrequency() {
        Order order = Order.builder().accountId("0").build(); // MM

        rule.validate(order);
        // Immediate second order should PASS for MM (1ms interval)
        assertDoesNotThrow(() -> rule.validate(order));
    }
}

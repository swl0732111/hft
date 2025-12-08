package com.hft.trading.service;

import com.hft.trading.domain.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

import com.hft.trading.service.risk.MaxNotionalValueRule;
import com.hft.trading.service.risk.MaxOrderQuantityRule;
import com.hft.trading.service.risk.RiskRule;

import java.util.Arrays;
import java.util.List;

class RiskControlServiceTest {

    private RiskControlService riskControlService;

    @BeforeEach
    void setUp() {
        List<RiskRule> rules = Arrays.asList(
                new MaxOrderQuantityRule(),
                new MaxNotionalValueRule());
        riskControlService = new RiskControlService(rules);
    }

    @Test
    void validateOrder_ShouldPassValidOrder() {
        Order order = Order.builder()
                .symbol("BTC/USDT")
                .build();
        order.setPriceFromDouble(50000.0);
        order.setQuantityFromDouble(1.0);

        assertDoesNotThrow(() -> riskControlService.validateOrder(order));
    }

    @Test
    void validateOrder_ShouldRejectLargeQuantity() {
        Order order = Order.builder()
                .symbol("BTC/USDT")
                .build();
        order.setPriceFromDouble(50000.0);
        order.setQuantityFromDouble(101.0); // Exceeds 100 limit

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> riskControlService.validateOrder(order));
        assertTrue(exception.getMessage().contains("Order quantity exceeds maximum limit"));
    }

    @Test
    void validateOrder_ShouldRejectLargeNotionalValue() {
        Order order = Order.builder()
                .symbol("BTC/USDT")
                .build();
        order.setPriceFromDouble(50000.0);
        order.setQuantityFromDouble(21.0); // 50000 * 21 = 1,050,000 > 1,000,000

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> riskControlService.validateOrder(order));
        assertTrue(exception.getMessage().contains("Order value exceeds maximum limit"));
    }

    @Test
    void validateOrder_ShouldThrowOnNullOrder() {
        assertThrows(IllegalArgumentException.class, () -> riskControlService.validateOrder(null));
    }
}

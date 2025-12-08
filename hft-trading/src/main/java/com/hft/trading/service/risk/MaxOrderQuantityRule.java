package com.hft.trading.service.risk;

import com.hft.trading.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MaxOrderQuantityRule implements RiskRule {

    private static final double MAX_ORDER_QUANTITY = 100.0;

    @Override
    public void validate(Order order) {
        double quantity = order.getQuantityAsDouble();
        if (quantity > MAX_ORDER_QUANTITY) {
            log.warn("Order rejected: Quantity {} exceeds limit {}", quantity, MAX_ORDER_QUANTITY);
            throw new IllegalArgumentException("Order quantity exceeds maximum limit of " + MAX_ORDER_QUANTITY);
        }
    }
}

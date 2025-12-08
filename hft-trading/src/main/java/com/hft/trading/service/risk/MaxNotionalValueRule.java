package com.hft.trading.service.risk;

import com.hft.trading.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MaxNotionalValueRule implements RiskRule {

    private static final double MAX_NOTIONAL_VALUE = 1_000_000.0;

    @Override
    public void validate(Order order) {
        double price = order.getPriceAsDouble();
        double quantity = order.getQuantityAsDouble();
        double notionalValue = price * quantity;

        if (notionalValue > MAX_NOTIONAL_VALUE) {
            log.warn("Order rejected: Notional value {} exceeds limit {}", notionalValue, MAX_NOTIONAL_VALUE);
            throw new IllegalArgumentException("Order value exceeds maximum limit of " + MAX_NOTIONAL_VALUE);
        }
    }
}

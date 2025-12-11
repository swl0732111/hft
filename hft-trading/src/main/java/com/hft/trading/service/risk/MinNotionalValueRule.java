package com.hft.trading.service.risk;

import com.hft.trading.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MinNotionalValueRule implements RiskRule {

    private static final double MIN_NOTIONAL_VALUE = 5.0; // Min order $5

    @Override
    public void validate(Order order) {
        double price = order.getPriceAsDouble();
        double quantity = order.getQuantityAsDouble();
        double notionalValue = price * quantity;

        if (notionalValue < MIN_NOTIONAL_VALUE) {
            // Check if it's a Market Maker (User 0) - maybe allow dust for MM?
            // For now, enforce for all.
            if ("0".equals(order.getAccountId())) {
                return; // Bypass for MM
            }

            log.warn("Order rejected: Notional value {} is below minimum {}", notionalValue, MIN_NOTIONAL_VALUE);
            throw new IllegalArgumentException(
                    "Order value too small (Dust). Minimum valid order value is " + MIN_NOTIONAL_VALUE);
        }
    }
}

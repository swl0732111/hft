package com.hft.trading.service.risk;

import com.hft.trading.domain.Order;

/**
 * Interface for risk control rules.
 */
public interface RiskRule {
    /**
     * Validate an order against this rule.
     *
     * @param order The order to validate
     * @throws IllegalArgumentException if the order violates the rule
     */
    void validate(Order order);
}

package com.hft.trading.service;

import com.hft.trading.domain.Order;
import com.hft.trading.service.risk.RiskRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskControlService {

    private final List<RiskRule> riskRules;

    /**
     * Validate order against risk rules.
     *
     * @param order The order to validate
     * @throws IllegalArgumentException if order violates risk rules
     */
    public void validateOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        for (RiskRule rule : riskRules) {
            rule.validate(order);
        }

        log.debug("Order {} passed risk checks", order.getId());
    }
}

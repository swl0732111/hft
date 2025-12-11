package com.hft.trading.service.risk;

import com.hft.trading.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limit the number of orders a user can place within a time window.
 * Using a simple "Minimum Time Interval" strategy (Leaky Bucket / Spacing
 * enforcement).
 * 
 * Policy:
 * - Retail Users: Min 100ms interval (10 orders/sec). (Strict)
 * - Market Makers (User 0): Min 1ms interval (1000 orders/sec).
 * - Others: Min 10ms interval (100 orders/sec).
 */
@Slf4j
@Component
public class OrderRateLimitRule implements RiskRule {

    private final Map<String, Long> lastOrderTimeMap = new ConcurrentHashMap<>();

    // Limits in milliseconds
    private static final long INTERVAL_RETAIL = 100; // 10 orders/sec
    private static final long INTERVAL_MM = 1; // 1000 orders/sec
    private static final long INTERVAL_DEFAULT = 10; // 100 orders/sec

    @Override
    public void validate(Order order) {
        String accountId = order.getAccountId();
        if (accountId == null)
            return;

        long now = System.currentTimeMillis();
        long lastTime = lastOrderTimeMap.getOrDefault(accountId, 0L);
        long interval = getInterval(accountId);

        if (now - lastTime < interval) {
            String msg = String.format("Rate limit exceeded for user %s. Min interval: %dms", accountId, interval);
            log.warn(msg);
            throw new IllegalArgumentException(msg);
        }

        lastOrderTimeMap.put(accountId, now);
    }

    private long getInterval(String accountId) {
        if ("0".equals(accountId) || "market_maker".equals(accountId)) {
            return INTERVAL_MM;
        }
        // Assuming "1" is our demo retail user. In prod, check user Roles.
        if ("1".equals(accountId)) {
            return INTERVAL_RETAIL; // Stricter for demo user to prevent spamming
        }
        return INTERVAL_DEFAULT;
    }
}

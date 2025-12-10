package com.hft.trading.api;

import com.hft.account.service.AccountService;
import com.hft.common.domain.AccountBalance;
import com.hft.trading.state.AccountStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * API for cache management and warming.
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final AccountService accountService;
    private final AccountStateStore accountStateStore;

    /**
     * Preload balances for a specific user.
     * Frontend can call this when user enters trading page.
     * 
     * @param accountId The account ID
     * @return Number of balances loaded
     */
    @PostMapping("/warm/{accountId}")
    public ResponseEntity<Map<String, Object>> warmUserCache(@PathVariable String accountId) {
        long startTime = System.currentTimeMillis();
        int count = 0;

        try {
            var balances = accountService.getAllBalances(accountId);
            for (AccountBalance balance : balances) {
                accountStateStore.updateBalance(balance);
                count++;
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Preloaded {} balances for account {} in {} ms", count, accountId, duration);

            Map<String, Object> response = new HashMap<>();
            response.put("accountId", accountId);
            response.put("balancesLoaded", count);
            response.put("durationMs", duration);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to warm cache for account {}", accountId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check cache status for debugging.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("strategy", "on-demand-per-user");
        status.put("message", "Cache loads automatically on first trade per user");
        return ResponseEntity.ok(status);
    }
}

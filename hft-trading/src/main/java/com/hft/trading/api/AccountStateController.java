package com.hft.trading.api;

import com.hft.trading.state.AccountState;
import com.hft.trading.state.EnhancedAccountStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API for querying real-time account state.
 * All data served from RocksDB for ultra-low latency.
 */
@Slf4j
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountStateController {

    private final EnhancedAccountStateStore stateStore;

    /**
     * Get complete account state including balances, positions, and risk metrics.
     * 
     * @param accountId The account ID
     * @return Complete account state from RocksDB cache
     */
    @GetMapping("/{accountId}/state")
    public ResponseEntity<AccountState> getAccountState(@PathVariable String accountId) {
        AccountState state = stateStore.getAccountState(accountId);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(state);
    }

    /**
     * Get risk metrics only.
     */
    @GetMapping("/{accountId}/risk")
    public ResponseEntity<AccountState.RiskMetrics> getRiskMetrics(@PathVariable String accountId) {
        AccountState state = stateStore.getAccountState(accountId);
        if (state == null || state.getRiskMetrics() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(state.getRiskMetrics());
    }

    /**
     * Get all positions.
     */
    @GetMapping("/{accountId}/positions")
    public ResponseEntity<?> getPositions(@PathVariable String accountId) {
        AccountState state = stateStore.getAccountState(accountId);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(state.getPositions());
    }

    /**
     * Get specific position.
     */
    @GetMapping("/{accountId}/positions/{symbol}")
    public ResponseEntity<AccountState.Position> getPosition(
            @PathVariable String accountId,
            @PathVariable String symbol) {
        AccountState state = stateStore.getAccountState(accountId);
        if (state == null || state.getPositions() == null) {
            return ResponseEntity.notFound().build();
        }

        AccountState.Position position = state.getPositions().get(symbol);
        if (position == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(position);
    }
}

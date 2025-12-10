package com.hft.dashboard.api;

import com.hft.dashboard.dto.AccountStateDTO;
import com.hft.dashboard.service.MockDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/** REST API controller for account state information. */
@Slf4j
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AccountController {

    private final MockDashboardService dashboardService;

    /** Get real-time account state (balances, positions, risk metrics). */
    @GetMapping("/{accountId}/state")
    public ResponseEntity<AccountStateDTO> getAccountState(@PathVariable String accountId) {
        log.info("Getting account state for: {}", accountId);

        // Create mock account state data
        Map<String, AccountStateDTO.AssetBalance> balances = new HashMap<>();
        balances.put("USDT", AccountStateDTO.AssetBalance.builder()
                .asset("USDT")
                .availableBalance(new BigDecimal("48500.00"))
                .lockedBalance(new BigDecimal("12500.00"))
                .totalBalance(new BigDecimal("61000.00"))
                .build());
        balances.put("BTC", AccountStateDTO.AssetBalance.builder()
                .asset("BTC")
                .availableBalance(new BigDecimal("0.8523"))
                .lockedBalance(new BigDecimal("0.1500"))
                .totalBalance(new BigDecimal("1.0023"))
                .build());
        balances.put("ETH", AccountStateDTO.AssetBalance.builder()
                .asset("ETH")
                .availableBalance(new BigDecimal("15.2345"))
                .lockedBalance(new BigDecimal("2.5000"))
                .totalBalance(new BigDecimal("17.7345"))
                .build());

        // Mock positions
        Map<String, AccountStateDTO.Position> positions = new HashMap<>();
        positions.put("BTC/USDT", AccountStateDTO.Position.builder()
                .symbol("BTC/USDT")
                .quantity(new BigDecimal("0.15"))
                .avgEntryPrice(new BigDecimal("42500.00"))
                .unrealizedPnL(new BigDecimal("425.50"))
                .realizedPnL(new BigDecimal("1250.00"))
                .openTime(System.currentTimeMillis() - 3600000 * 8) // 8 hours ago
                .build());
        positions.put("ETH/USDT", AccountStateDTO.Position.builder()
                .symbol("ETH/USDT")
                .quantity(new BigDecimal("2.5"))
                .avgEntryPrice(new BigDecimal("2850.00"))
                .unrealizedPnL(new BigDecimal("-125.30"))
                .realizedPnL(new BigDecimal("680.50"))
                .openTime(System.currentTimeMillis() - 3600000 * 12) // 12 hours ago
                .build());

        // Mock risk metrics
        AccountStateDTO.RiskMetrics riskMetrics = AccountStateDTO.RiskMetrics.builder()
                .marginLevel(new BigDecimal("245.50"))
                .totalEquity(new BigDecimal("63500.00"))
                .availableMargin(new BigDecimal("48500.00"))
                .leverage(new BigDecimal("2.5"))
                .build();

        AccountStateDTO accountState = AccountStateDTO.builder()
                .balances(balances)
                .positions(positions)
                .riskMetrics(riskMetrics)
                .build();

        return ResponseEntity.ok(accountState);
    }
}

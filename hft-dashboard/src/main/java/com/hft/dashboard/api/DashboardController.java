package com.hft.dashboard.api;

import com.hft.dashboard.dto.*;
import com.hft.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST API controller for user dashboard. */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allow frontend access
public class DashboardController {

  private final DashboardService dashboardService;

  /**
   * Get dashboard overview with trading statistics.
   *
   * @param accountId User account ID
   * @return Dashboard overview with 24h, 7d, 30d statistics
   */
  @GetMapping("/overview")
  public ResponseEntity<DashboardOverviewDTO> getOverview(@RequestParam String accountId) {
    log.info("Getting dashboard overview for account: {}", accountId);
    DashboardOverviewDTO overview = dashboardService.getOverview(accountId);
    return ResponseEntity.ok(overview);
  }

  /**
   * Get tier information and progress.
   *
   * @param accountId User account ID
   * @return Tier info with progress to next tier
   */
  @GetMapping("/tier-info")
  public ResponseEntity<TierInfoDTO> getTierInfo(@RequestParam String accountId) {
    log.info("Getting tier info for account: {}", accountId);
    TierInfoDTO tierInfo = dashboardService.getTierInfo(accountId);
    return ResponseEntity.ok(tierInfo);
  }

  /**
   * Get 30-day volume chart data.
   *
   * @param accountId User account ID
   * @return Volume chart data with daily volumes and tier thresholds
   */
  @GetMapping("/volume-chart")
  public ResponseEntity<VolumeChartDTO> getVolumeChart(@RequestParam String accountId) {
    log.info("Getting volume chart for account: {}", accountId);
    VolumeChartDTO volumeChart = dashboardService.getVolumeChart(accountId);
    return ResponseEntity.ok(volumeChart);
  }

  /**
   * Get trade distribution analytics.
   *
   * @param accountId User account ID
   * @return Trade distribution with hourly patterns, pair distribution, and buy/sell ratio
   */
  @GetMapping("/trade-distribution")
  public ResponseEntity<TradeDistributionDTO> getTradeDistribution(@RequestParam String accountId) {
    log.info("Getting trade distribution for account: {}", accountId);
    TradeDistributionDTO distribution =
        ((com.hft.dashboard.service.MockDashboardService) dashboardService)
            .getTradeDistribution(accountId);
    return ResponseEntity.ok(distribution);
  }

  /**
   * Get profit/loss analysis.
   *
   * @param accountId User account ID
   * @return P&L data with daily breakdown, cumulative stats, and fee analysis
   */
  @GetMapping("/profit-loss")
  public ResponseEntity<ProfitLossDTO> getProfitLoss(@RequestParam String accountId) {
    log.info("Getting profit/loss data for account: {}", accountId);
    ProfitLossDTO profitLoss =
        ((com.hft.dashboard.service.MockDashboardService) dashboardService)
            .getProfitLoss(accountId);
    return ResponseEntity.ok(profitLoss);
  }

  /**
   * Get performance metrics.
   *
   * @param accountId User account ID
   * @return Performance metrics including win rate, holding time, drawdown, and risk metrics
   */
  @GetMapping("/performance-metrics")
  public ResponseEntity<PerformanceMetricsDTO> getPerformanceMetrics(
      @RequestParam String accountId) {
    log.info("Getting performance metrics for account: {}", accountId);
    PerformanceMetricsDTO metrics =
        ((com.hft.dashboard.service.MockDashboardService) dashboardService)
            .getPerformanceMetrics(accountId);
    return ResponseEntity.ok(metrics);
  }

  /** Health check endpoint. */
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Dashboard API is running");
  }

  // --- Wallet Endpoints ---

  @GetMapping("/wallet/balance")
  public ResponseEntity<WalletBalanceDTO> getWalletBalance(@RequestParam String accountId) {
    log.info("Getting wallet balance for account: {}", accountId);
    WalletBalanceDTO balance =
        ((com.hft.dashboard.service.MockDashboardService) dashboardService)
            .getWalletBalance(accountId);
    return ResponseEntity.ok(balance);
  }

  @GetMapping("/wallet/transactions")
  public ResponseEntity<java.util.List<WalletTransactionDTO>> getWalletTransactions(
      @RequestParam String accountId) {
    log.info("Getting wallet transactions for account: {}", accountId);
    java.util.List<WalletTransactionDTO> transactions =
        ((com.hft.dashboard.service.MockDashboardService) dashboardService)
            .getWalletTransactions(accountId);
    return ResponseEntity.ok(transactions);
  }

  @PostMapping("/wallet/deposit")
  public ResponseEntity<WalletTransactionDTO> deposit(@RequestBody TransactionRequestDTO request) {
    log.info("Processing deposit for account: {}", request.getAccountId());
    request.setType("DEPOSIT");
    WalletTransactionDTO transaction =
        ((com.hft.dashboard.service.MockDashboardService) dashboardService)
            .processWalletTransaction(request);
    return ResponseEntity.ok(transaction);
  }

  @PostMapping("/wallet/withdraw")
  public ResponseEntity<WalletTransactionDTO> withdraw(@RequestBody TransactionRequestDTO request) {
    log.info("Processing withdrawal for account: {}", request.getAccountId());
    request.setType("WITHDRAWAL");
    WalletTransactionDTO transaction =
        ((com.hft.dashboard.service.MockDashboardService) dashboardService)
            .processWalletTransaction(request);
    return ResponseEntity.ok(transaction);
  }
}

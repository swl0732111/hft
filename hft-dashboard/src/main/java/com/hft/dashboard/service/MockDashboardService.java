package com.hft.dashboard.service;

import com.hft.common.domain.UserTier;
import com.hft.dashboard.dto.*;
import com.hft.wallet.service.Web3WalletService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Mock dashboard service for demonstration purposes. Returns sample data when
 * actual database is
 * not available.
 */
@Slf4j
@Service
@Primary
public class MockDashboardService extends DashboardService {

  private static final String PLATFORM_HOT_WALLET_ADDRESS = "0x71C7656EC7ab88b098defB751B7401B5f6d8976F";
  private final Map<String, WalletBalanceDTO> mockBalances = new HashMap<>();
  private final Map<String, List<WalletTransactionDTO>> mockTransactions = new HashMap<>();
  @Autowired
  private Web3WalletService web3WalletService;

  public MockDashboardService() {
    super(null, null, null, null, null);
  }

  @Override
  public DashboardOverviewDTO getOverview(String accountId) {
    log.info("Returning mock overview data for account: {}", accountId);

    return DashboardOverviewDTO.builder()
        .accountId(accountId)
        .stats24h(createMockStats(150000, 45))
        .stats7d(createMockStats(980000, 287))
        .stats30d(createMockStats(4250000, 1156))
        .build();
  }

  @Override
  public TierInfoDTO getTierInfo(String accountId) {
    log.info("Returning mock tier info for account: {}", accountId);

    UserTier currentTier = UserTier.VIP2;
    UserTier nextTier = UserTier.VIP3;
    double volume30d = 4250000;
    double volumeToNextTier = 750000;
    double progressPercent = 85.0;

    return TierInfoDTO.builder()
        .currentTier(currentTier)
        .tierLevel(currentTier.getLevel())
        .volume30d(volume30d)
        .nextTier(nextTier)
        .volumeToNextTier(volumeToNextTier)
        .progressPercent(progressPercent)
        .makerFeeBps(currentTier.getMakerFeeBps())
        .takerFeeBps(currentTier.getTakerFeeBps())
        .makerFeePercent(currentTier.getMakerFeePercent())
        .takerFeePercent(currentTier.getTakerFeePercent())
        .feeSavings(1275.50)
        .lastTierUpdate(System.currentTimeMillis() - (15L * 24 * 60 * 60 * 1000)) // 15 days ago
        .tierLocked(false)
        .tierLockedUntil(null)
        .benefits(createMockBenefits())
        .build();
  }

  @Override
  public VolumeChartDTO getVolumeChart(String accountId) {
    log.info("Returning mock volume chart for account: {}", accountId);

    LocalDate today = LocalDate.now();
    List<VolumeChartDTO.DailyVolumeDTO> dailyData = new ArrayList<>();

    // Generate 30 days of mock data with some variation
    Random random = new Random(42); // Fixed seed for consistent data
    for (int i = 0; i < 30; i++) {
      LocalDate date = today.minusDays(29 - i);
      double baseVolume = 120000 + random.nextDouble() * 80000;
      int tradeCount = 30 + random.nextInt(50);

      dailyData.add(
          VolumeChartDTO.DailyVolumeDTO.builder()
              .date(date.toString())
              .volume(baseVolume)
              .tradeCount(tradeCount)
              .build());
    }

    // Build tier thresholds
    List<VolumeChartDTO.TierThresholdDTO> thresholds = Arrays.asList(
        VolumeChartDTO.TierThresholdDTO.builder().tier("VIP1").volume(1000000.0).build(),
        VolumeChartDTO.TierThresholdDTO.builder().tier("VIP2").volume(3500000.0).build(),
        VolumeChartDTO.TierThresholdDTO.builder().tier("VIP3").volume(5000000.0).build(),
        VolumeChartDTO.TierThresholdDTO.builder().tier("VIP4").volume(10000000.0).build());

    return VolumeChartDTO.builder().data(dailyData).tierThresholds(thresholds).build();
  }

  private DashboardOverviewDTO.TradingStatsDTO createMockStats(double volume, int tradeCount) {
    double avgTradeSize = volume / tradeCount;
    double feesPaid = volume * 0.0008; // 8 bps
    double feeSavings = volume * 0.0002; // 2 bps savings

    return DashboardOverviewDTO.TradingStatsDTO.builder()
        .volume(volume)
        .tradeCount(tradeCount)
        .avgTradeSize(avgTradeSize)
        .feesPaid(feesPaid)
        .feeSavings(feeSavings)
        .build();
  }

  private TierInfoDTO.TierBenefitsDTO createMockBenefits() {
    return TierInfoDTO.TierBenefitsDTO.builder()
        .apiRateLimit("50 req/s")
        .supportPriority("High")
        .priorityWithdrawal(true)
        .dedicatedAccountManager(false)
        .customApiSolutions(false)
        .build();
  }

  /** Get trade distribution analytics. */
  public TradeDistributionDTO getTradeDistribution(String accountId) {
    log.info("Returning mock trade distribution for account: {}", accountId);

    Random random = new Random(42);

    // Hourly distribution (24 hours)
    List<TradeDistributionDTO.HourlyDistribution> hourlyDist = new ArrayList<>();
    for (int hour = 0; hour < 24; hour++) {
      // Simulate higher activity during market hours (9-16)
      double multiplier = (hour >= 9 && hour <= 16) ? 1.5 : 0.7;
      double volume = (50000 + random.nextDouble() * 30000) * multiplier;
      int tradeCount = (int) ((10 + random.nextInt(20)) * multiplier);

      hourlyDist.add(
          TradeDistributionDTO.HourlyDistribution.builder()
              .hour(hour)
              .volume(volume)
              .tradeCount(tradeCount)
              .avgTradeSize(volume / tradeCount)
              .build());
    }

    // Trading pair distribution
    List<TradeDistributionDTO.PairDistribution> pairDist = Arrays.asList(
        TradeDistributionDTO.PairDistribution.builder()
            .symbol("BTC/USDT")
            .volume(1800000)
            .percentage(42.35)
            .tradeCount(450)
            .build(),
        TradeDistributionDTO.PairDistribution.builder()
            .symbol("ETH/USDT")
            .volume(1200000)
            .percentage(28.24)
            .tradeCount(380)
            .build(),
        TradeDistributionDTO.PairDistribution.builder()
            .symbol("SOL/USDT")
            .volume(650000)
            .percentage(15.29)
            .tradeCount(220)
            .build(),
        TradeDistributionDTO.PairDistribution.builder()
            .symbol("AVAX/USDT")
            .volume(400000)
            .percentage(9.41)
            .tradeCount(106)
            .build(),
        TradeDistributionDTO.PairDistribution.builder()
            .symbol("Others")
            .volume(200000)
            .percentage(4.71)
            .tradeCount(100)
            .build());

    // Buy/Sell ratio
    TradeDistributionDTO.BuySellRatio buySellRatio = TradeDistributionDTO.BuySellRatio.builder()
        .buyVolume(2300000)
        .sellVolume(1950000)
        .buyCount(620)
        .sellCount(536)
        .buyPercentage(54.12)
        .sellPercentage(45.88)
        .build();

    return TradeDistributionDTO.builder()
        .hourlyDistribution(hourlyDist)
        .pairDistribution(pairDist)
        .buySellRatio(buySellRatio)
        .build();
  }

  /** Get trade history for an account. */
  @Override
  public List<TradeHistoryDTO> getTradeHistory(String accountId) {
    log.info("Returning mock trade history for account: {}", accountId);

    List<TradeHistoryDTO> history = new ArrayList<>();
    Random random = new Random(42);
    long currentTime = System.currentTimeMillis();

    // Generate 20 mock trades
    for (int i = 0; i < 20; i++) {
      String symbol = random.nextBoolean() ? "BTC/USDT" : "ETH/USDT";
      String type = random.nextBoolean() ? "BUY" : "SELL";
      double price = "BTC/USDT".equals(symbol) ? 50000 + random.nextDouble() * 1000 : 3000 + random.nextDouble() * 100;
      double amount = "BTC/USDT".equals(symbol) ? 0.1 + random.nextDouble() * 0.5 : 1.0 + random.nextDouble() * 5.0;

      history.add(TradeHistoryDTO.builder()
          .transactionId(UUID.randomUUID().toString())
          .orderId(UUID.randomUUID().toString())
          .symbol(symbol)
          .type(type)
          .amount(amount)
          .price(price)
          .status("FILLED")
          .timestamp(currentTime - (long) (random.nextDouble() * 7 * 24 * 60 * 60 * 1000)) // Random time in last 7 days
          .txHash("0x" + UUID.randomUUID().toString().replace("-", "").substring(0, 16))
          .asset(symbol.split("/")[0])
          .build());
    }

    history.sort(Comparator.comparingLong(TradeHistoryDTO::getTimestamp).reversed());
    return history;
  }

  // --- Wallet Mock Data ---

  /** Get profit/loss analysis. */
  public ProfitLossDTO getProfitLoss(String accountId) {
    log.info("Returning mock profit/loss data for account: {}", accountId);

    LocalDate today = LocalDate.now();
    Random random = new Random(42);
    List<ProfitLossDTO.DailyPnL> dailyPnL = new ArrayList<>();
    List<ProfitLossDTO.DailyFee> dailyFees = new ArrayList<>();

    double cumulativeRealized = 0;
    int profitableDays = 0;
    int losingDays = 0;

    // Generate 30 days of P&L data
    for (int i = 0; i < 30; i++) {
      LocalDate date = today.minusDays(29 - i);
      double realizedPnL = (random.nextDouble() - 0.45) * 5000; // Slightly profitable bias
      double unrealizedPnL = (random.nextDouble() - 0.5) * 2000;
      double feesPaid = 100 + random.nextDouble() * 150;

      cumulativeRealized += realizedPnL;
      if (realizedPnL > 0)
        profitableDays++;
      else
        losingDays++;

      dailyPnL.add(
          ProfitLossDTO.DailyPnL.builder()
              .date(date.toString())
              .realizedPnL(realizedPnL)
              .unrealizedPnL(unrealizedPnL)
              .totalPnL(realizedPnL + unrealizedPnL)
              .feesPaid(feesPaid)
              .netPnL(realizedPnL + unrealizedPnL - feesPaid)
              .build());

      dailyFees.add(
          ProfitLossDTO.DailyFee.builder()
              .date(date.toString())
              .feesPaid(feesPaid)
              .feeSavings(feesPaid * 0.25) // 25% savings from VIP tier
              .build());
    }

    double totalFees = dailyFees.stream().mapToDouble(ProfitLossDTO.DailyFee::getFeesPaid).sum();
    double totalSavings = dailyFees.stream().mapToDouble(ProfitLossDTO.DailyFee::getFeeSavings).sum();

    ProfitLossDTO.CumulativePnL cumulativePnL = ProfitLossDTO.CumulativePnL.builder()
        .totalRealized(cumulativeRealized)
        .totalUnrealized(500) // Current open positions
        .totalFees(totalFees)
        .netProfit(cumulativeRealized - totalFees)
        .roi(((cumulativeRealized - totalFees) / 100000) * 100) // Assuming 100k initial capital
        .profitableDays(profitableDays)
        .losingDays(losingDays)
        .build();

    ProfitLossDTO.FeeAnalysis feeAnalysis = ProfitLossDTO.FeeAnalysis.builder()
        .totalFeesPaid(totalFees)
        .totalFeeSavings(totalSavings)
        .avgFeePerTrade(totalFees / 1156) // Total trades from overview
        .feeAsPercentOfVolume((totalFees / 4250000) * 100)
        .dailyFees(dailyFees)
        .build();

    return ProfitLossDTO.builder()
        .dailyPnL(dailyPnL)
        .cumulativePnL(cumulativePnL)
        .feeAnalysis(feeAnalysis)
        .build();
  }

  /** Get performance metrics. */
  public PerformanceMetricsDTO getPerformanceMetrics(String accountId) {
    log.info("Returning mock performance metrics for account: {}", accountId);

    // Win rate stats
    PerformanceMetricsDTO.WinRateStats winRate = PerformanceMetricsDTO.WinRateStats.builder()
        .totalTrades(1156)
        .profitableTrades(687)
        .losingTrades(469)
        .winRate(59.43)
        .avgWin(850.50)
        .avgLoss(-520.30)
        .profitFactor(1.63) // Total profit / Total loss
        .build();

    // Holding time distribution
    List<PerformanceMetricsDTO.HoldingTimeDistribution> holdingTime = Arrays.asList(
        PerformanceMetricsDTO.HoldingTimeDistribution.builder()
            .timeRange("< 1 min")
            .tradeCount(234)
            .percentage(20.24)
            .avgPnL(120.50)
            .build(),
        PerformanceMetricsDTO.HoldingTimeDistribution.builder()
            .timeRange("1-5 min")
            .tradeCount(389)
            .percentage(33.65)
            .avgPnL(250.30)
            .build(),
        PerformanceMetricsDTO.HoldingTimeDistribution.builder()
            .timeRange("5-30 min")
            .tradeCount(312)
            .percentage(26.99)
            .avgPnL(380.75)
            .build(),
        PerformanceMetricsDTO.HoldingTimeDistribution.builder()
            .timeRange("30 min - 1 hr")
            .tradeCount(145)
            .percentage(12.54)
            .avgPnL(520.40)
            .build(),
        PerformanceMetricsDTO.HoldingTimeDistribution.builder()
            .timeRange("> 1 hr")
            .tradeCount(76)
            .percentage(6.57)
            .avgPnL(680.90)
            .build());

    // Drawdown periods
    List<PerformanceMetricsDTO.DrawdownPeriod> drawdownPeriods = Arrays.asList(
        PerformanceMetricsDTO.DrawdownPeriod.builder()
            .startDate(LocalDate.now().minusDays(25).toString())
            .endDate(LocalDate.now().minusDays(20).toString())
            .drawdownPercent(-8.5)
            .durationDays(5)
            .build(),
        PerformanceMetricsDTO.DrawdownPeriod.builder()
            .startDate(LocalDate.now().minusDays(12).toString())
            .endDate(LocalDate.now().minusDays(8).toString())
            .drawdownPercent(-12.3)
            .durationDays(4)
            .build());

    PerformanceMetricsDTO.DrawdownAnalysis drawdown = PerformanceMetricsDTO.DrawdownAnalysis.builder()
        .maxDrawdown(-12500)
        .maxDrawdownPercent(-12.3)
        .maxDrawdownDate(LocalDate.now().minusDays(10).toString())
        .currentDrawdown(-2300)
        .drawdownDays(2)
        .drawdownPeriods(drawdownPeriods)
        .build();

    // Risk metrics
    PerformanceMetricsDTO.RiskMetrics riskMetrics = PerformanceMetricsDTO.RiskMetrics.builder()
        .sharpeRatio(1.85)
        .volatility(0.15)
        .maxConsecutiveLosses(5)
        .maxConsecutiveWins(8)
        .avgTradeRisk(0.02) // 2% per trade
        .build();

    return PerformanceMetricsDTO.builder()
        .winRate(winRate)
        .holdingTimeDistribution(holdingTime)
        .drawdown(drawdown)
        .riskMetrics(riskMetrics)
        .build();
  }

  private void initMockWallet(String accountId) {
    if (!mockBalances.containsKey(accountId)) {
      mockBalances.put(
          accountId,
          WalletBalanceDTO.builder()
              .asset("USDT")
              .availableBalance(new BigDecimal("50000.00"))
              .lockedBalance(new BigDecimal("12500.00"))
              .totalBalance(new BigDecimal("62500.00"))
              .usdValue(new BigDecimal("62500.00"))
              .build());
    }
    if (!mockTransactions.containsKey(accountId)) {
      List<WalletTransactionDTO> txs = new ArrayList<>();
      // Add some initial history
      txs.add(
          WalletTransactionDTO.builder()
              .transactionId(UUID.randomUUID().toString())
              .type("DEPOSIT")
              .asset("USDT")
              .amount(new BigDecimal("10000.00"))
              .status("COMPLETED")
              .timestamp(System.currentTimeMillis() - 86400000L * 5)
              .txHash("0x123...abc")
              .build());
      txs.add(
          WalletTransactionDTO.builder()
              .transactionId(UUID.randomUUID().toString())
              .type("WITHDRAWAL")
              .asset("USDT")
              .amount(new BigDecimal("5000.00"))
              .status("COMPLETED")
              .timestamp(System.currentTimeMillis() - 86400000L * 2)
              .txHash("0x456...def")
              .build());
      mockTransactions.put(accountId, txs);
    }
  }

  public WalletBalanceDTO getWalletBalance(String accountId) {
    initMockWallet(accountId);
    return mockBalances.get(accountId);
  }

  public List<WalletTransactionDTO> getWalletTransactions(String accountId) {
    initMockWallet(accountId);
    return mockTransactions.get(accountId);
  }

  public WalletTransactionDTO processWalletTransaction(TransactionRequestDTO request) {
    // Verify signature
    if (request.getSignature() == null
        || request.getWalletAddress() == null
        || request.getNonce() == null) {
      throw new RuntimeException("Wallet signature, address, and nonce are required");
    }

    // Allow bypass for mock wallet
    if ("MOCK_SIGNATURE_BYPASS".equals(request.getSignature())) {
      log.info("Mock signature bypass used for wallet: {}", request.getWalletAddress());
    } else {
      boolean isValid = web3WalletService.verifySignature(
          request.getWalletAddress(), request.getSignature(), request.getNonce());

      if (!isValid) {
        throw new RuntimeException("Invalid wallet signature");
      }
      log.info("Signature verified for wallet: {}", request.getWalletAddress());
    }

    initMockWallet(request.getAccountId());
    WalletBalanceDTO balance = mockBalances.get(request.getAccountId());
    List<WalletTransactionDTO> history = mockTransactions.get(request.getAccountId());

    String txHash = "0x" + UUID.randomUUID().toString().replace("-", "");

    WalletTransactionDTO tx = WalletTransactionDTO.builder()
        .transactionId(UUID.randomUUID().toString())
        .type(request.getType())
        .asset(request.getAsset())
        .amount(request.getAmount())
        .timestamp(System.currentTimeMillis())
        .txHash(txHash)
        .build();

    if ("DEPOSIT".equals(request.getType())) {
      log.info(
          "Processing DEPOSIT: {} {} from {} to Hot Wallet {}",
          request.getAmount(),
          request.getAsset(),
          request.getWalletAddress(),
          PLATFORM_HOT_WALLET_ADDRESS);

      balance.setAvailableBalance(balance.getAvailableBalance().add(request.getAmount()));
      balance.setTotalBalance(balance.getTotalBalance().add(request.getAmount()));
      balance.setUsdValue(balance.getTotalBalance()); // Assuming 1:1 for USDT
      tx.setStatus("COMPLETED");

      // --- Cashback Logic ---
      BigDecimal cashbackRate = new BigDecimal("0.05"); // 5% cashback
      BigDecimal cashbackAmount = request.getAmount().multiply(cashbackRate);

      if (cashbackAmount.compareTo(BigDecimal.ZERO) > 0) {
        log.info("Applying cashback of {} {} for account {}", cashbackAmount, request.getAsset(),
            request.getAccountId());

        WalletTransactionDTO cashbackTx = WalletTransactionDTO.builder()
            .transactionId(UUID.randomUUID().toString())
            .type("CASHBACK")
            .asset(request.getAsset())
            .amount(cashbackAmount)
            .timestamp(System.currentTimeMillis())
            .status("COMPLETED")
            .txHash("0x" + UUID.randomUUID().toString().replace("-", "")) // Mock hash
            .build();

        // Update balance with cashback
        balance.setAvailableBalance(balance.getAvailableBalance().add(cashbackAmount));
        balance.setTotalBalance(balance.getTotalBalance().add(cashbackAmount));
        balance.setUsdValue(balance.getTotalBalance());

        // Add to history
        history.add(0, cashbackTx);
      }
      // ---------------------
    } else if ("WITHDRAWAL".equals(request.getType())) {
      if (balance.getAvailableBalance().compareTo(request.getAmount()) >= 0) {
        log.info(
            "Processing WITHDRAWAL: {} {} from Hot Wallet {} to {}",
            request.getAmount(),
            request.getAsset(),
            PLATFORM_HOT_WALLET_ADDRESS,
            request.getWalletAddress());

        balance.setAvailableBalance(balance.getAvailableBalance().subtract(request.getAmount()));
        balance.setTotalBalance(balance.getTotalBalance().subtract(request.getAmount()));
        balance.setUsdValue(balance.getTotalBalance());
        tx.setStatus("COMPLETED");
      } else {
        tx.setStatus("FAILED"); // Insufficient funds
      }
    }

    history.add(0, tx); // Add to top
    return tx;
  }
}

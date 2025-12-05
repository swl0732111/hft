package com.hft.dashboard.service;

import com.hft.common.domain.Account;
import com.hft.common.domain.TierConfig;
import com.hft.common.domain.UserTier;
import com.hft.common.util.FixedPointMath;
import com.hft.dashboard.dto.*;
import com.hft.trading.domain.TradingVolumeStats;
import com.hft.trading.repository.AccountRepository;
import com.hft.trading.repository.TradingVolumeStatsRepository;
import com.hft.trading.service.TierService;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Service for aggregating dashboard data. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

  private final AccountRepository accountRepository;
  private final TradingVolumeStatsRepository volumeStatsRepository;
  private final TierService tierService;

  /** Get dashboard overview with trading statistics. */
  public DashboardOverviewDTO getOverview(String accountId) {
    LocalDate today = LocalDate.now();

    // Get stats for different periods
    var stats24h = getStatsForPeriod(accountId, today.minusDays(1), today);
    var stats7d = getStatsForPeriod(accountId, today.minusDays(7), today);
    var stats30d = getStatsForPeriod(accountId, today.minusDays(30), today);

    return DashboardOverviewDTO.builder()
        .accountId(accountId)
        .stats24h(stats24h)
        .stats7d(stats7d)
        .stats30d(stats30d)
        .build();
  }

  /** Get tier information and progress. */
  public TierInfoDTO getTierInfo(String accountId) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));

    UserTier currentTier = account.getCurrentTierOrDefault();
    UserTier nextTier = currentTier.getNextTier();
    long volume30dScaled = account.getVolume30dScaled();
    double volume30d = FixedPointMath.toDouble(volume30dScaled);

    // Calculate progress to next tier
    double volumeToNextTier = 0;
    double progressPercent = 0;
    if (nextTier != null) {
      long volumeToNextScaled = nextTier.getMinVolumeScaled() - volume30dScaled;
      volumeToNextTier = FixedPointMath.toDouble(volumeToNextScaled);

      long tierRange = nextTier.getMinVolumeScaled() - currentTier.getMinVolumeScaled();
      long volumeInTier = volume30dScaled - currentTier.getMinVolumeScaled();
      progressPercent = (double) volumeInTier / tierRange * 100.0;
      progressPercent = Math.min(100.0, Math.max(0.0, progressPercent));
    } else {
      progressPercent = 100.0; // At max tier
    }

    // Calculate fee savings compared to VIP0
    double feeSavings = calculateFeeSavings(accountId, currentTier);

    // Get tier config for benefits
    TierConfig tierConfig = tierService.getTierConfig(currentTier);

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
        .feeSavings(feeSavings)
        .lastTierUpdate(account.getLastTierUpdate())
        .tierLocked(account.isTierLocked())
        .tierLockedUntil(account.getTierLockedUntil())
        .benefits(buildBenefitsDTO(tierConfig))
        .build();
  }

  /** Get 30-day volume chart data. */
  public VolumeChartDTO getVolumeChart(String accountId) {
    LocalDate today = LocalDate.now();
    LocalDate startDate = today.minusDays(30);

    List<TradingVolumeStats> stats =
        volumeStatsRepository.findByAccountIdAndDateBetween(accountId, startDate, today);

    // Create map for quick lookup
    Map<LocalDate, TradingVolumeStats> statsMap =
        stats.stream().collect(Collectors.toMap(TradingVolumeStats::getDate, s -> s));

    // Build daily volume data (fill missing days with 0)
    List<VolumeChartDTO.DailyVolumeDTO> dailyData = new ArrayList<>();
    for (int i = 0; i < 30; i++) {
      LocalDate date = startDate.plusDays(i);
      TradingVolumeStats stat = statsMap.get(date);

      double volume = 0;
      int tradeCount = 0;
      if (stat != null) {
        volume = FixedPointMath.toDouble(stat.getVolumeScaled());
        tradeCount = stat.getTradeCount();
      }

      dailyData.add(
          VolumeChartDTO.DailyVolumeDTO.builder()
              .date(date.toString())
              .volume(volume)
              .tradeCount(tradeCount)
              .build());
    }

    // Build tier thresholds
    List<VolumeChartDTO.TierThresholdDTO> thresholds =
        Arrays.stream(UserTier.values())
            .filter(tier -> tier != UserTier.VIP0) // Skip VIP0 (starts at 0)
            .map(
                tier ->
                    VolumeChartDTO.TierThresholdDTO.builder()
                        .tier(tier.name())
                        .volume(tier.getMinVolume())
                        .build())
            .collect(Collectors.toList());

    return VolumeChartDTO.builder().data(dailyData).tierThresholds(thresholds).build();
  }

  /** Get trading statistics for a specific period. */
  private DashboardOverviewDTO.TradingStatsDTO getStatsForPeriod(
      String accountId, LocalDate startDate, LocalDate endDate) {

    List<TradingVolumeStats> stats =
        volumeStatsRepository.findByAccountIdAndDateBetween(accountId, startDate, endDate);

    long totalVolumeScaled = stats.stream().mapToLong(TradingVolumeStats::getVolumeScaled).sum();

    int totalTrades = stats.stream().mapToInt(TradingVolumeStats::getTradeCount).sum();

    double volume = FixedPointMath.toDouble(totalVolumeScaled);
    double avgTradeSize = totalTrades > 0 ? volume / totalTrades : 0;

    // TODO: Calculate actual fees paid from trade history
    // For now, estimate based on volume and current tier
    Account account = accountRepository.findById(accountId).orElse(null);
    UserTier tier = account != null ? account.getCurrentTierOrDefault() : UserTier.VIP0;
    double estimatedFees = volume * (tier.getTakerFeeBps() / 10000.0); // Rough estimate

    double feeSavings = calculateFeeSavingsForVolume(volume, tier);

    return DashboardOverviewDTO.TradingStatsDTO.builder()
        .volume(volume)
        .tradeCount(totalTrades)
        .avgTradeSize(avgTradeSize)
        .feesPaid(estimatedFees)
        .feeSavings(feeSavings)
        .build();
  }

  /** Calculate fee savings compared to VIP0. */
  private double calculateFeeSavings(String accountId, UserTier currentTier) {
    long volume30dScaled =
        volumeStatsRepository.sumVolumeByAccountIdAndDateBetween(
            accountId, LocalDate.now().minusDays(30), LocalDate.now());

    double volume30d = FixedPointMath.toDouble(volume30dScaled);
    return calculateFeeSavingsForVolume(volume30d, currentTier);
  }

  /** Calculate fee savings for a given volume and tier. */
  private double calculateFeeSavingsForVolume(double volume, UserTier tier) {
    if (tier == UserTier.VIP0) {
      return 0;
    }

    double vip0Fee = volume * (UserTier.VIP0.getTakerFeeBps() / 10000.0);
    double currentFee = volume * (tier.getTakerFeeBps() / 10000.0);
    return vip0Fee - currentFee;
  }

  /** Build benefits DTO from tier config. */
  private TierInfoDTO.TierBenefitsDTO buildBenefitsDTO(TierConfig config) {
    return TierInfoDTO.TierBenefitsDTO.builder()
        .apiRateLimit(config.getApiRateLimitRps() + " req/s")
        .supportPriority(getSupportPriorityLabel(config.getSupportPriority()))
        .priorityWithdrawal(config.isPriorityWithdrawal())
        .dedicatedAccountManager(config.isDedicatedAccountManager())
        .customApiSolutions(config.isCustomApiSolutions())
        .build();
  }

  /** Get support priority label. */
  private String getSupportPriorityLabel(int priority) {
    return switch (priority) {
      case 1 -> "Highest";
      case 2 -> "High";
      case 3 -> "Medium";
      case 4 -> "Normal";
      default -> "Standard";
    };
  }
}

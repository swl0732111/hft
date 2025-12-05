package com.hft.dashboard.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/** DTO for performance metrics and statistics. */
@Data
@Builder
public class PerformanceMetricsDTO {

  /** Win rate statistics. */
  private WinRateStats winRate;

  /** Holding time distribution. */
  private List<HoldingTimeDistribution> holdingTimeDistribution;

  /** Drawdown analysis. */
  private DrawdownAnalysis drawdown;

  /** Risk metrics. */
  private RiskMetrics riskMetrics;

  @Data
  @Builder
  public static class WinRateStats {
    private int totalTrades;
    private int profitableTrades;
    private int losingTrades;
    private double winRate; // % of profitable trades
    private double avgWin; // Average profit per winning trade
    private double avgLoss; // Average loss per losing trade
    private double profitFactor; // Total profit / Total loss
  }

  @Data
  @Builder
  public static class HoldingTimeDistribution {
    private String timeRange; // e.g., "< 1min", "1-5min", "5-30min", etc.
    private int tradeCount;
    private double percentage;
    private double avgPnL; // Average P&L for this time range
  }

  @Data
  @Builder
  public static class DrawdownAnalysis {
    private double maxDrawdown; // Maximum drawdown amount
    private double maxDrawdownPercent; // Maximum drawdown %
    private String maxDrawdownDate; // When max drawdown occurred
    private double currentDrawdown; // Current drawdown from peak
    private int drawdownDays; // Days in current drawdown
    private List<DrawdownPeriod> drawdownPeriods;
  }

  @Data
  @Builder
  public static class DrawdownPeriod {
    private String startDate;
    private String endDate;
    private double drawdownPercent;
    private int durationDays;
  }

  @Data
  @Builder
  public static class RiskMetrics {
    private double sharpeRatio; // Risk-adjusted return
    private double volatility; // Standard deviation of returns
    private double maxConsecutiveLosses;
    private double maxConsecutiveWins;
    private double avgTradeRisk; // Average risk per trade
  }
}

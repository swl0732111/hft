package com.hft.dashboard.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/** DTO for profit/loss analysis. */
@Data
@Builder
public class ProfitLossDTO {

  /** Daily profit/loss data for the period. */
  private List<DailyPnL> dailyPnL;

  /** Cumulative profit/loss. */
  private CumulativePnL cumulativePnL;

  /** Fee analysis. */
  private FeeAnalysis feeAnalysis;

  @Data
  @Builder
  public static class DailyPnL {
    private String date;
    private double realizedPnL; // Actual profit/loss from closed positions
    private double unrealizedPnL; // Current open positions P&L
    private double totalPnL; // realized + unrealized
    private double feesPaid;
    private double netPnL; // totalPnL - feesPaid
  }

  @Data
  @Builder
  public static class CumulativePnL {
    private double totalRealized;
    private double totalUnrealized;
    private double totalFees;
    private double netProfit;
    private double roi; // Return on Investment %
    private int profitableDays;
    private int losingDays;
  }

  @Data
  @Builder
  public static class FeeAnalysis {
    private double totalFeesPaid;
    private double totalFeeSavings; // Savings from VIP tier
    private double avgFeePerTrade;
    private double feeAsPercentOfVolume;
    private List<DailyFee> dailyFees;
  }

  @Data
  @Builder
  public static class DailyFee {
    private String date;
    private double feesPaid;
    private double feeSavings;
  }
}

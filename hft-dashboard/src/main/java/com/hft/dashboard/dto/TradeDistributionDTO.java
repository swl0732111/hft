package com.hft.dashboard.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/** DTO for trade distribution analytics. */
@Data
@Builder
public class TradeDistributionDTO {

  /** Hourly trading volume distribution (24 hours). */
  private List<HourlyDistribution> hourlyDistribution;

  /** Trading pair distribution. */
  private List<PairDistribution> pairDistribution;

  /** Buy/Sell ratio. */
  private BuySellRatio buySellRatio;

  @Data
  @Builder
  public static class HourlyDistribution {
    private int hour; // 0-23
    private double volume;
    private int tradeCount;
    private double avgTradeSize;
  }

  @Data
  @Builder
  public static class PairDistribution {
    private String symbol; // e.g., "BTC/USDT"
    private double volume;
    private double percentage; // % of total volume
    private int tradeCount;
  }

  @Data
  @Builder
  public static class BuySellRatio {
    private double buyVolume;
    private double sellVolume;
    private int buyCount;
    private int sellCount;
    private double buyPercentage; // % of total volume
    private double sellPercentage; // % of total volume
  }
}

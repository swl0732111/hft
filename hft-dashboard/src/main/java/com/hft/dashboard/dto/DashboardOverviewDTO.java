package com.hft.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for dashboard overview statistics. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDTO {
  private String accountId;
  private TradingStatsDTO stats24h;
  private TradingStatsDTO stats7d;
  private TradingStatsDTO stats30d;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TradingStatsDTO {
    private double volume;
    private int tradeCount;
    private double avgTradeSize;
    private double feesPaid;
    private double feeSavings; // Compared to VIP0
  }
}

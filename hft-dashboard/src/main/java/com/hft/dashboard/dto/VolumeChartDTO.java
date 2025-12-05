package com.hft.dashboard.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for 30-day volume chart data. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolumeChartDTO {
  private List<DailyVolumeDTO> data;
  private List<TierThresholdDTO> tierThresholds;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DailyVolumeDTO {
    private String date; // YYYY-MM-DD
    private double volume;
    private int tradeCount;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TierThresholdDTO {
    private String tier;
    private double volume;
  }
}

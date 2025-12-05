package com.hft.trading.domain;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Daily trading volume statistics for tier calculation. Tracks volume per account per day for
 * rolling 30-day calculations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("trading_volume_stats")
public class TradingVolumeStats {
  @Id private String id;

  private String accountId;
  private LocalDate date;

  // Total trading volume for this day (scaled long format)
  // Includes both buy and sell side volume
  private long volumeScaled;

  // Number of trades executed
  private int tradeCount;

  // Metadata
  private long createdAt;
  private long updatedAt;

  /** Create new stats entry for today. */
  public static TradingVolumeStats createForToday(String accountId) {
    long now = System.currentTimeMillis();
    return TradingVolumeStats.builder()
        .accountId(accountId)
        .date(LocalDate.now())
        .volumeScaled(0L)
        .tradeCount(0)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  /**
   * Increment volume for a trade.
   *
   * @param tradeVolumeScaled Trade volume to add (price * quantity)
   */
  public void addVolume(long tradeVolumeScaled) {
    this.volumeScaled += tradeVolumeScaled;
    this.tradeCount++;
    this.updatedAt = System.currentTimeMillis();
  }
}

package com.hft.trading.domain;

import com.hft.common.domain.UserTier;
import com.hft.common.util.FixedPointMath;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Tiered fee configuration based on user tier and trading symbol. Replaces the single-tier
 * FeeConfig with per-tier fee rates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("tiered_fee_config")
public class TieredFeeConfig {
  @Id private String id;

  private String symbol;
  private UserTier tier;

  // Fee rates (in basis points, e.g., 10 = 0.1%)
  private int makerFeeBps;
  private int takerFeeBps;

  // Minimum fee in quote currency
  private BigDecimal minFee;

  private boolean active;
  private long createdAt;
  private long updatedAt;

  /** Create default tiered fee config from UserTier. */
  public static TieredFeeConfig createDefault(String symbol, UserTier tier) {
    long now = System.currentTimeMillis();
    return TieredFeeConfig.builder()
        .symbol(symbol)
        .tier(tier)
        .makerFeeBps(tier.getMakerFeeBps())
        .takerFeeBps(tier.getTakerFeeBps())
        .minFee(BigDecimal.valueOf(0.01))
        .active(true)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  /**
   * Calculate maker fee for a trade.
   *
   * @param tradeValue Total trade value (price * quantity)
   * @return Fee amount
   */
  public BigDecimal calculateMakerFee(BigDecimal tradeValue) {
    BigDecimal fee =
        tradeValue
            .multiply(BigDecimal.valueOf(makerFeeBps))
            .divide(BigDecimal.valueOf(10000), 8, RoundingMode.HALF_UP);
    return fee.max(minFee);
  }

  /**
   * Calculate taker fee for a trade.
   *
   * @param tradeValue Total trade value (price * quantity)
   * @return Fee amount
   */
  public BigDecimal calculateTakerFee(BigDecimal tradeValue) {
    BigDecimal fee =
        tradeValue
            .multiply(BigDecimal.valueOf(takerFeeBps))
            .divide(BigDecimal.valueOf(10000), 8, RoundingMode.HALF_UP);
    return fee.max(minFee);
  }

  /**
   * Calculate fee using scaled long arithmetic (zero allocation).
   *
   * @param tradeValueScaled Trade value in scaled format
   * @param isMaker True if maker, false if taker
   * @return Fee in scaled format
   */
  public long calculateFeeScaled(long tradeValueScaled, boolean isMaker) {
    int feeBps = isMaker ? makerFeeBps : takerFeeBps;
    // Fee = tradeValue * feeBps / 10000
    long feeScaled = (tradeValueScaled * feeBps) / 10000;

    // Apply minimum fee
    long minFeeScaled = FixedPointMath.fromDouble(minFee.doubleValue());
    return Math.max(feeScaled, minFeeScaled);
  }
}

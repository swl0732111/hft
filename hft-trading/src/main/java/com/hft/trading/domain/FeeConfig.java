package com.hft.trading.domain;

import com.hft.common.util.FixedPointMath;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

/**
 * Fee configuration for trading pairs.
 * Supports tiered fee structure based on trading volume.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("fee_config")
public class FeeConfig {
    @Id
    private String id;

    private String symbol;

    // Fee rates (in basis points, e.g., 10 = 0.1%)
    private int makerFeeBps; // Fee for providing liquidity (maker)
    private int takerFeeBps; // Fee for taking liquidity (taker)

    // Minimum fee in quote currency
    private BigDecimal minFee;

    // VIP tier (0 = default, 1-5 = VIP levels)
    private int tier;

    private boolean active;

    /**
     * Calculate maker fee for a trade.
     * 
     * @param tradeValue Total trade value (price * quantity)
     * @return Fee amount
     */
    public BigDecimal calculateMakerFee(BigDecimal tradeValue) {
        BigDecimal fee = tradeValue.multiply(BigDecimal.valueOf(makerFeeBps))
                .divide(BigDecimal.valueOf(10000), 8, BigDecimal.ROUND_HALF_UP);
        return fee.max(minFee);
    }

    /**
     * Calculate taker fee for a trade.
     * 
     * @param tradeValue Total trade value (price * quantity)
     * @return Fee amount
     */
    public BigDecimal calculateTakerFee(BigDecimal tradeValue) {
        BigDecimal fee = tradeValue.multiply(BigDecimal.valueOf(takerFeeBps))
                .divide(BigDecimal.valueOf(10000), 8, BigDecimal.ROUND_HALF_UP);
        return fee.max(minFee);
    }

    /**
     * Calculate fee using scaled long arithmetic (zero allocation).
     * 
     * @param tradeValueScaled Trade value in scaled format
     * @param isMaker          True if maker, false if taker
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

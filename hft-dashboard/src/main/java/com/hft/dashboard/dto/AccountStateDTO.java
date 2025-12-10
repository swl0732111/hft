package com.hft.dashboard.dto;

import java.math.BigDecimal;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountStateDTO {
    private Map<String, AssetBalance> balances;
    private Map<String, Position> positions;
    private RiskMetrics riskMetrics;

    @Data
    @Builder
    public static class AssetBalance {
        private String asset;
        private BigDecimal availableBalance;
        private BigDecimal lockedBalance;
        private BigDecimal totalBalance;
    }

    @Data
    @Builder
    public static class Position {
        private String symbol;
        private BigDecimal quantity;
        private BigDecimal avgEntryPrice;
        private BigDecimal unrealizedPnL;
        private BigDecimal realizedPnL;
        private Long openTime;
    }

    @Data
    @Builder
    public static class RiskMetrics {
        private BigDecimal marginLevel;
        private BigDecimal totalEquity;
        private BigDecimal availableMargin;
        private BigDecimal leverage;
    }
}

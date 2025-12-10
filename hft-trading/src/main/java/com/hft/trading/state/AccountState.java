package com.hft.trading.state;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive account state cached in RocksDB.
 * Stores real-time trading state for ultra-low latency access.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountState {

    private String accountId;

    // 资产余额（按币种）
    @Builder.Default
    private Map<String, AssetBalance> balances = new HashMap<>();

    // 持仓信息（按交易对）
    @Builder.Default
    private Map<String, Position> positions = new HashMap<>();

    // 风险指标
    private RiskMetrics riskMetrics;

    // 最后更新时间
    private long lastUpdateTime;

    /**
     * 单个资产的余额
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetBalance {
        private String asset;
        private BigDecimal availableBalance;
        private BigDecimal lockedBalance;
        private BigDecimal totalBalance; // available + locked
    }

    /**
     * 持仓信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private String symbol; // 交易对 (e.g., BTC-USDC)
        private BigDecimal quantity; // 持仓量
        private BigDecimal avgEntryPrice; // 平均开仓价格
        private BigDecimal unrealizedPnL; // 未实现盈亏
        private BigDecimal realizedPnL; // 已实现盈亏
        private long openTime; // 开仓时间
    }

    /**
     * 风险指标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskMetrics {
        // 保证金相关
        private BigDecimal initialMargin; // 初始保证金
        private BigDecimal maintenanceMargin; // 维持保证金
        private BigDecimal marginLevel; // 保证金率 (%)

        // 权益相关
        private BigDecimal totalEquity; // 总权益
        private BigDecimal availableMargin; // 可用保证金

        // 风险指标
        private BigDecimal leverage; // 当前杠杆倍数
        private BigDecimal exposureValue; // 敞口价值
        private BigDecimal maxDrawdown; // 最大回撤

        // 限制
        private BigDecimal maxLeverage; // 最大允许杠杆
        private BigDecimal riskLimit; // 风险限额
    }

    /**
     * 计算总权益
     */
    public BigDecimal calculateTotalEquity() {
        BigDecimal equity = BigDecimal.ZERO;

        // 所有资产余额
        for (AssetBalance balance : balances.values()) {
            equity = equity.add(balance.getTotalBalance());
        }

        // 加上所有持仓的未实现盈亏
        for (Position position : positions.values()) {
            if (position.getUnrealizedPnL() != null) {
                equity = equity.add(position.getUnrealizedPnL());
            }
        }

        return equity;
    }

    /**
     * 更新风险指标
     */
    public void updateRiskMetrics() {
        if (riskMetrics == null) {
            riskMetrics = new RiskMetrics();
        }

        // 计算总权益
        BigDecimal totalEquity = calculateTotalEquity();
        riskMetrics.setTotalEquity(totalEquity);

        // 计算可用保证金
        BigDecimal availableMargin = totalEquity.subtract(
                riskMetrics.getInitialMargin() != null ? riskMetrics.getInitialMargin() : BigDecimal.ZERO);
        riskMetrics.setAvailableMargin(availableMargin);

        // 计算保证金率
        if (riskMetrics.getMaintenanceMargin() != null &&
                riskMetrics.getMaintenanceMargin().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal marginLevel = totalEquity
                    .divide(riskMetrics.getMaintenanceMargin(), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            riskMetrics.setMarginLevel(marginLevel);
        }
    }

    /**
     * 添加或更新资产余额
     */
    public void updateBalance(String asset, BigDecimal available, BigDecimal locked) {
        AssetBalance balance = balances.computeIfAbsent(asset, k -> new AssetBalance());
        balance.setAsset(asset);
        balance.setAvailableBalance(available);
        balance.setLockedBalance(locked);
        balance.setTotalBalance(available.add(locked));
        this.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * 添加或更新持仓
     */
    public void updatePosition(String symbol, Position position) {
        positions.put(symbol, position);
        this.lastUpdateTime = System.currentTimeMillis();
        updateRiskMetrics();
    }

    /**
     * 关闭持仓
     */
    public void closePosition(String symbol) {
        positions.remove(symbol);
        this.lastUpdateTime = System.currentTimeMillis();
        updateRiskMetrics();
    }
}

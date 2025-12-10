package com.hft.aggregator.domain;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Result of smart order routing decision.
 */
@Data
@Builder
public class RoutingDecision {

    private final String orderId;
    private final String symbol;
    private final Side side; // BUY or SELL
    private final double totalQuantity;

    // Primary routing
    private final List<RouteAllocation> allocations;

    // Backup routing (for failover)
    private final List<RouteAllocation> backupAllocations;

    // Execution metadata
    private final double expectedAvgPrice;
    private final double totalFees;
    private final double totalCost; // Price + fees
    private final long estimatedLatencyMs;

    // Risk assessment
    private final RiskLevel riskLevel;
    private final String riskReason;

    public enum Side {
        BUY, SELL
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    /**
     * Single allocation to a liquidity source.
     */
    @Data
    @Builder
    public static class RouteAllocation {
        private final String sourceId;
        private final double quantity;
        private final double price;
        private final double feeBps;
        private final long estimatedLatencyMs;

        public double getTotalCost() {
            return quantity * price * (1 + feeBps / 10000.0);
        }
    }

    /**
     * Check if routing is feasible.
     */
    public boolean isFeasible() {
        return !allocations.isEmpty() && riskLevel != RiskLevel.HIGH;
    }

    /**
     * Get primary sources.
     */
    public List<String> getPrimarySources() {
        return allocations.stream()
                .map(RouteAllocation::getSourceId)
                .toList();
    }

    /**
     * Get backup sources.
     */
    public List<String> getBackupSources() {
        return backupAllocations != null ? backupAllocations.stream()
                .map(RouteAllocation::getSourceId)
                .toList() : List.of();
    }
}

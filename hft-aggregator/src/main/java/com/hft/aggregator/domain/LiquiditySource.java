package com.hft.aggregator.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata for a liquidity source (exchange or LP provider).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiquiditySource {

    private String id; // e.g., "binance", "okx"
    private SourceType type; // EXCHANGE or LP
    private String websocketUrl;
    private String restUrl;
    private boolean enabled;
    private int priority; // Lower number = higher priority

    // Performance metrics
    private volatile long avgLatencyNanos; // Exponential moving average
    private volatile double reliabilityScore; // 0.0 - 1.0
    private volatile long lastSuccessTimestamp;
    private volatile long consecutiveFailures;

    // Fee structure
    private int makerFeeBps; // Maker fee in basis points
    private int takerFeeBps; // Taker fee in basis points

    // Connection state
    private volatile ConnectionState connectionState;
    private volatile long lastReconnectAttempt;

    public enum SourceType {
        EXCHANGE,
        LP_PROVIDER
    }

    public enum ConnectionState {
        CONNECTED,
        CONNECTING,
        DISCONNECTED,
        DEGRADED // Connected but experiencing issues
    }

    /**
     * Update latency using exponential moving average.
     */
    public void updateLatency(long latencyNanos) {
        if (avgLatencyNanos == 0) {
            avgLatencyNanos = latencyNanos;
        } else {
            // EMA with alpha = 0.1
            avgLatencyNanos = (long) (0.9 * avgLatencyNanos + 0.1 * latencyNanos);
        }
    }

    /**
     * Record successful operation.
     */
    public void recordSuccess() {
        lastSuccessTimestamp = System.currentTimeMillis();
        consecutiveFailures = 0;
        // Improve reliability score
        reliabilityScore = Math.min(1.0, reliabilityScore + 0.01);
    }

    /**
     * Record failed operation.
     */
    public void recordFailure() {
        consecutiveFailures++;
        // Degrade reliability score
        reliabilityScore = Math.max(0.0, reliabilityScore - 0.05);
    }

    /**
     * Check if source is healthy enough for routing.
     */
    public boolean isHealthy() {
        return enabled
                && connectionState == ConnectionState.CONNECTED
                && reliabilityScore > 0.5
                && consecutiveFailures < 3;
    }

    /**
     * Calculate overall score for source selection.
     */
    public double calculateScore(double latencyWeight, double reliabilityWeight) {
        if (!isHealthy()) {
            return 0.0;
        }

        // Normalize latency (lower is better, normalize to 0-1 range)
        double latencyScore = 1.0 / (1.0 + avgLatencyNanos / 1_000_000.0); // Convert to ms

        return latencyWeight * latencyScore + reliabilityWeight * reliabilityScore;
    }
}

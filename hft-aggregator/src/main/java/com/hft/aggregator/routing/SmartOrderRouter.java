package com.hft.aggregator.routing;

import com.hft.aggregator.aggregator.OrderBookAggregator;
import com.hft.aggregator.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Order Router - Main SOR engine.
 * Determines optimal order routing across multiple liquidity sources.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmartOrderRouter {

    private final OrderBookAggregator orderBookAggregator;
    private final LiquidityScorer liquidityScorer;
    private final PriceOptimizer priceOptimizer;
    private final LatencyController latencyController;
    private final FailoverManager failoverManager;
    private final com.hft.aggregator.edgecase.CircuitBreaker circuitBreaker;

    /**
     * Route an order to optimal liquidity sources.
     */
    public RoutingDecision route(OrderRequest request) {
        long startNanos = System.nanoTime();

        try {
            // 0. Check Circuit Breaker
            if (!circuitBreaker.allowRequest()) {
                return buildInvalidDecision(request, "Circuit Breaker Tripped: " + circuitBreaker.getState());
            }

            // 1. Validate request
            if (!validateRequest(request)) {
                return buildInvalidDecision(request, "Invalid order request");
            }

            // 2. Get aggregated order book
            int symbolId = com.hft.aggregator.domain.SymbolDictionary.getId(request.getSymbol());
            if (symbolId == -1) {
                return buildInvalidDecision(request, "Unknown symbol/Order book not available");
            }
            com.hft.aggregator.domain.ArrayOrderBook orderBook = orderBookAggregator.getOrderBook(symbolId);
            if (orderBook == null) {
                return buildInvalidDecision(request, "Order book not available");
            }

            // 3. Select candidate liquidity sources
            List<LiquiditySource> candidates = liquidityScorer.selectSources(
                    request.getSymbol(),
                    request.getQuantity(),
                    request.getSide() == RoutingDecision.Side.BUY);

            if (candidates.isEmpty()) {
                return buildInvalidDecision(request, "No healthy liquidity sources available");
            }

            // 4. Optimize price and allocate across sources
            List<RoutingDecision.RouteAllocation> allocations = priceOptimizer.optimize(
                    request,
                    orderBook,
                    candidates);

            if (allocations.isEmpty()) {
                return buildInvalidDecision(request, "Unable to find executable routing");
            }

            // 5. Adjust for latency constraints
            allocations = latencyController.adjustForLatency(allocations, request.getMaxLatencyMs());

            // 6. Add failover/backup routing
            List<RoutingDecision.RouteAllocation> backupAllocations = failoverManager.generateBackup(
                    allocations,
                    candidates);

            // 7. Calculate execution metrics
            double avgPrice = calculateWeightedAvgPrice(allocations);
            double totalFees = calculateTotalFees(allocations);
            long estimatedLatency = allocations.stream()
                    .mapToLong(RoutingDecision.RouteAllocation::getEstimatedLatencyMs)
                    .max()
                    .orElse(0);

            // 8. Assess risk
            RoutingDecision.RiskLevel riskLevel = assessRisk(request, allocations, orderBook);
            String riskReason = getRiskReason(riskLevel, allocations);

            long totalLatencyNanos = System.nanoTime() - startNanos;
            log.debug("SOR decision for {} {} {} completed in {}μs",
                    request.getSide(), request.getQuantity(), request.getSymbol(),
                    totalLatencyNanos / 1000);

            return RoutingDecision.builder()
                    .orderId(request.getOrderId())
                    .symbol(request.getSymbol())
                    .side(request.getSide())
                    .totalQuantity(request.getQuantity())
                    .allocations(allocations)
                    .backupAllocations(backupAllocations)
                    .expectedAvgPrice(avgPrice)
                    .totalFees(totalFees)
                    .totalCost(avgPrice * request.getQuantity() + totalFees)
                    .estimatedLatencyMs(estimatedLatency)
                    .riskLevel(riskLevel)
                    .riskReason(riskReason)
                    .build();

        } catch (Exception e) {
            log.error("Error routing order: {}", request, e);
            return buildInvalidDecision(request, "Internal routing error: " + e.getMessage());
        }
    }

    private boolean validateRequest(OrderRequest request) {
        return request != null
                && request.getSymbol() != null
                && request.getQuantity() > 0
                && request.getSide() != null;
    }

    private RoutingDecision buildInvalidDecision(OrderRequest request, String reason) {
        return RoutingDecision.builder()
                .orderId(request != null ? request.getOrderId() : "unknown")
                .symbol(request != null ? request.getSymbol() : "unknown")
                .side(request != null ? request.getSide() : RoutingDecision.Side.BUY)
                .totalQuantity(request != null ? request.getQuantity() : 0)
                .allocations(Collections.emptyList())
                .backupAllocations(Collections.emptyList())
                .riskLevel(RoutingDecision.RiskLevel.HIGH)
                .riskReason(reason)
                .build();
    }

    private double calculateWeightedAvgPrice(List<RoutingDecision.RouteAllocation> allocations) {
        double totalValue = 0;
        double totalQuantity = 0;
        for (RoutingDecision.RouteAllocation allocation : allocations) {
            totalValue += allocation.getPrice() * allocation.getQuantity();
            totalQuantity += allocation.getQuantity();
        }
        return totalQuantity > 0 ? totalValue / totalQuantity : 0;
    }

    private double calculateTotalFees(List<RoutingDecision.RouteAllocation> allocations) {
        return allocations.stream()
                .mapToDouble(a -> a.getQuantity() * a.getPrice() * a.getFeeBps() / 10000.0)
                .sum();
    }

    private RoutingDecision.RiskLevel assessRisk(OrderRequest request,
            List<RoutingDecision.RouteAllocation> allocations,
            com.hft.aggregator.domain.ArrayOrderBook orderBook) {
        // Check if order is large relative to available liquidity
        double availableLiq = orderBook.getAvailableLiquidity(
                request.getSide() == RoutingDecision.Side.BUY,
                request.getQuantity() * 2);

        if (request.getQuantity() > availableLiq * 0.8) {
            return RoutingDecision.RiskLevel.HIGH; // Order consumes >80% of liquidity
        }

        if (request.getQuantity() > availableLiq * 0.5) {
            return RoutingDecision.RiskLevel.MEDIUM; // Order consumes >50% of liquidity
        }

        // Check if routing is fragmented across many sources
        if (allocations.size() > 3) {
            return RoutingDecision.RiskLevel.MEDIUM; // Fragmented execution
        }

        return RoutingDecision.RiskLevel.LOW;
    }

    private String getRiskReason(RoutingDecision.RiskLevel level,
            List<RoutingDecision.RouteAllocation> allocations) {
        if (level == RoutingDecision.RiskLevel.HIGH) {
            return "Large order relative to available liquidity";
        } else if (level == RoutingDecision.RiskLevel.MEDIUM) {
            if (allocations.size() > 3) {
                return "Execution fragmented across " + allocations.size() + " sources";
            }
            return "Moderate liquidity consumption";
        }
        return "Low risk";
    }

    /**
     * Order request model.
     */
    public static class OrderRequest {
        private final String orderId;
        private final String symbol;
        private final double quantity;
        private final RoutingDecision.Side side;
        private final long maxLatencyMs;

        public OrderRequest(String orderId, String symbol, double quantity,
                RoutingDecision.Side side, long maxLatencyMs) {
            this.orderId = orderId;
            this.symbol = symbol;
            this.quantity = quantity;
            this.side = side;
            this.maxLatencyMs = maxLatencyMs;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getSymbol() {
            return symbol;
        }

        public double getQuantity() {
            return quantity;
        }

        public RoutingDecision.Side getSide() {
            return side;
        }

        public long getMaxLatencyMs() {
            return maxLatencyMs;
        }
    }
}

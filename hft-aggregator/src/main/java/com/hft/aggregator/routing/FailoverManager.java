package com.hft.aggregator.routing;

import com.hft.aggregator.domain.LiquiditySource;
import com.hft.aggregator.domain.RoutingDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages failover and backup routing.
 */
@Slf4j
@Component
public class FailoverManager {

    /**
     * Generate backup allocations for failover.
     */
    public List<RoutingDecision.RouteAllocation> generateBackup(
            List<RoutingDecision.RouteAllocation> primaryAllocations,
            List<LiquiditySource> availableSources) {

        // Get sources used in primary routing
        Set<String> primarySources = primaryAllocations.stream()
                .map(RoutingDecision.RouteAllocation::getSourceId)
                .collect(Collectors.toSet());

        // Find backup sources (not in primary)
        List<LiquiditySource> backupSources = availableSources.stream()
                .filter(s -> !primarySources.contains(s.getId()))
                .filter(LiquiditySource::isHealthy)
                .limit(2) // Up to 2 backup sources
                .collect(Collectors.toList());

        if (backupSources.isEmpty()) {
            log.warn("No backup sources available for failover");
            return Collections.emptyList();
        }

        // Create backup allocations (simplified - equal split)
        double totalQty = primaryAllocations.stream()
                .mapToDouble(RoutingDecision.RouteAllocation::getQuantity)
                .sum();

        return backupSources.stream()
                .map(source -> RoutingDecision.RouteAllocation.builder()
                        .sourceId(source.getId())
                        .quantity(totalQty / backupSources.size())
                        .price(0) // Price will be determined at execution time
                        .feeBps(source.getTakerFeeBps())
                        .estimatedLatencyMs(source.getAvgLatencyNanos() / 1_000_000)
                        .build())
                .collect(Collectors.toList());
    }
}

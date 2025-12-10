package com.hft.aggregator.routing;

import com.hft.aggregator.domain.RoutingDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controls latency in order routing decisions.
 */
@Slf4j
@Component
public class LatencyController {

    /**
     * Adjust allocations to meet latency constraints.
     */
    public List<RoutingDecision.RouteAllocation> adjustForLatency(
            List<RoutingDecision.RouteAllocation> allocations,
            long maxLatencyMs) {

        if (maxLatencyMs <= 0) {
            return allocations; // No latency constraint
        }

        // Filter out allocations that exceed latency threshold
        List<RoutingDecision.RouteAllocation> filtered = allocations.stream()
                .filter(a -> a.getEstimatedLatencyMs() <= maxLatencyMs)
                .collect(Collectors.toList());

        if (filtered.size() < allocations.size()) {
            log.info("Filtered {} allocations due to latency constraint (max: {}ms)",
                    allocations.size() - filtered.size(), maxLatencyMs);
        }

        return filtered;
    }
}

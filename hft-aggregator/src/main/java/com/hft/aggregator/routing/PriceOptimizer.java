package com.hft.aggregator.routing;

import com.hft.aggregator.domain.*;
import com.hft.aggregator.routing.SmartOrderRouter.OrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Optimizes order execution price across multiple sources.
 */
@Slf4j
@Component
public class PriceOptimizer {

    /**
     * Optimize order allocation for best price execution.
     */
    public List<RoutingDecision.RouteAllocation> optimize(
            OrderRequest request,
            ArrayOrderBook orderBook,
            List<LiquiditySource> sources) {

        List<RoutingDecision.RouteAllocation> allocations = new ArrayList<>();
        double remainingQty = request.getQuantity();
        double scale = 100_000_000.0;

        // Iterate through price levels and allocate
        // ArrayOrderBook has raw arrays.
        boolean isBuy = request.getSide() == RoutingDecision.Side.BUY;
        int count = isBuy ? orderBook.getAskCount() : orderBook.getBidCount();
        int limit = Math.min(count, 20); // Top 20 levels

        for (int i = 0; i < limit; i++) {
            if (remainingQty <= 0)
                break;

            long priceScaled = isBuy ? orderBook.getAskPrice(i) : orderBook.getBidPrice(i);
            long qtyScaled = isBuy ? orderBook.getAskQuantity(i) : orderBook.getBidQuantity(i);
            int sourceId = isBuy ? orderBook.getAskSourceId(i) : orderBook.getBidSourceId(i);

            String sourceName = SymbolDictionary.getSymbol(sourceId); // Using dictionary to get source name

            // Find source info
            LiquiditySource source = null;
            if (sourceName != null) {
                source = sources.stream()
                        .filter(s -> s.getId().equals(sourceName))
                        .findFirst()
                        .orElse(null);
            }

            if (source == null || !source.isHealthy()) {
                continue;
            }

            double levelQty = qtyScaled / scale;
            double levelPrice = priceScaled / scale;

            double allocQty = Math.min(remainingQty, levelQty);
            int feeBps = isBuy ? source.getTakerFeeBps() : source.getTakerFeeBps(); // Simplified

            allocations.add(RoutingDecision.RouteAllocation.builder()
                    .sourceId(source.getId())
                    .quantity(allocQty)
                    .price(levelPrice)
                    .feeBps(feeBps)
                    .estimatedLatencyMs(source.getAvgLatencyNanos() / 1_000_000)
                    .build());

            remainingQty -= allocQty;
        }

        if (remainingQty > 0) {
            log.warn("Unable to allocate full quantity for {}: {} remaining",
                    request.getSymbol(), remainingQty);
        }

        return allocations;
    }
}

package com.hft.aggregator.routing;

import com.hft.aggregator.aggregator.OrderBookAggregator;
import com.hft.aggregator.domain.RoutingDecision;
import com.hft.aggregator.edgecase.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmartOrderRouterTest {

    private SmartOrderRouter router;
    private CircuitBreaker circuitBreaker;
    private OrderBookAggregator aggregator;
    private LiquidityScorer scorer;
    private PriceOptimizer optimizer;
    private LatencyController latencyController;
    private FailoverManager failoverManager;

    @BeforeEach
    void setUp() {
        circuitBreaker = mock(CircuitBreaker.class);
        aggregator = mock(OrderBookAggregator.class);
        scorer = mock(LiquidityScorer.class);
        optimizer = mock(PriceOptimizer.class);
        latencyController = mock(LatencyController.class);
        failoverManager = mock(FailoverManager.class);

        router = new SmartOrderRouter(aggregator, scorer, optimizer, latencyController, failoverManager,
                circuitBreaker);
    }

    @Test
    void testRouteRejectsWhenCircuitOpen() {
        when(circuitBreaker.allowRequest()).thenReturn(false);
        when(circuitBreaker.getState())
                .thenReturn(new java.util.concurrent.atomic.AtomicReference<>(CircuitBreaker.State.OPEN));

        SmartOrderRouter.OrderRequest request = new SmartOrderRouter.OrderRequest(
                "1", "BTC/USDT", 1.0, RoutingDecision.Side.BUY, 100);

        RoutingDecision decision = router.route(request);

        assertNotNull(decision);
        assertEquals(RoutingDecision.RiskLevel.HIGH, decision.getRiskLevel());
        assertTrue(decision.getRiskReason().contains("Circuit Breaker Tripped"));

        verify(aggregator, never()).getOrderBook(anyString());
    }

    // Additional tests for actual routing would require mocking complex
    // interactions,
    // which is better suited for a larger test suite or Integration tests.
    // For now, this confirms the breaker Integration.
}

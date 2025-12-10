package com.hft.aggregator.connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hft.aggregator.disruptor.DisruptorService;
import com.hft.aggregator.domain.OrderBookLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CoinbaseConnectorTest {

    private CoinbaseConnector connector;
    private DisruptorService disruptorService;

    @BeforeEach
    void setUp() {
        disruptorService = mock(DisruptorService.class);
        connector = new CoinbaseConnector(disruptorService);
    }

    @Test
    void testHandleSnapshotParsing() throws Exception {
        // Snapshot processing is currently deferred in Disruptor implementation.
        // We verified parsing previously, but now we just check it doesn't crash.

        String json = "{" +
                "\"type\":\"snapshot\"," +
                "\"product_id\":\"BTC-USD\"," +
                "\"bids\":[[\"50000.50\",\"1.5\"],[\"50000.00\",\"2.0\"]]," +
                "\"asks\":[[\"50100.00\",\"1.0\"]]" +
                "}";

        Method onMessage = CoinbaseConnector.class.getMethod("onMessage", String.class);
        onMessage.invoke(connector, json);

        // No IO to disruptor expected for snapshot yet per current implementation plan
        verifyNoInteractions(disruptorService);
    }

    @Test
    void testHandleL2UpdateParsing() throws Exception {
        String json = "{" +
                "\"type\":\"l2update\"," +
                "\"product_id\":\"ETH-USD\"," +
                "\"changes\":[" +
                "[\"buy\",\"3000.00\",\"5.0\"]," +
                "[\"sell\",\"3005.00\",\"10.0\"]" +
                "]" +
                "}";

        Method onMessage = CoinbaseConnector.class.getMethod("onMessage", String.class);
        onMessage.invoke(connector, json);

        // Expected: internal symbol logic mapping "ETH-USD" -> "ETH-USD" as subscribe
        // wasn't called to map it
        // Or default. Let's assume default mapping or explicit check.

        verify(disruptorService).publish(eq("ETH-USD"), eq(3000_00000000L), eq(5_00000000L), eq("coinbase"), eq(true));
        verify(disruptorService).publish(eq("ETH-USD"), eq(3005_00000000L), eq(10_00000000L), eq("coinbase"),
                eq(false));
    }
}

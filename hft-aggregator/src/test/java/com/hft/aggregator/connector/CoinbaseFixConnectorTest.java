package com.hft.aggregator.connector;

import com.hft.aggregator.disruptor.DisruptorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.field.*;
import quickfix.fix42.MarketDataIncrementalRefresh;
import quickfix.fix42.MarketDataSnapshotFullRefresh;
import quickfix.SessionID;

import static org.mockito.Mockito.*;

class CoinbaseFixConnectorTest {

    private CoinbaseFixConnector connector;
    private DisruptorService disruptorService;

    @BeforeEach
    void setUp() {
        disruptorService = mock(DisruptorService.class);
        connector = new CoinbaseFixConnector(disruptorService);
    }

    @Test
    void testHandleSnapshot() throws Exception {
        // Construct FIX 4.2 Snapshot message
        MarketDataSnapshotFullRefresh message = new MarketDataSnapshotFullRefresh();
        message.set(new Symbol("BTC-USD"));

        message.addGroup(createGroup(MDEntryType.BID, 50000.0, 1.0));
        message.addGroup(createGroup(MDEntryType.OFFER, 50100.0, 2.0));

        // Process
        connector.fromApp(message, new SessionID("FIX.4.2", "SENDER", "TARGET"));

        // Verify NO interaction for snapshot in current implementation
        verifyNoInteractions(disruptorService);
    }

    @Test
    void testHandleIncremental() throws Exception {
        // Construct FIX 4.2 Incremental Refresh
        MarketDataIncrementalRefresh message = new MarketDataIncrementalRefresh();

        // Update Bid: 50000 -> 50001 (New)
        MarketDataIncrementalRefresh.NoMDEntries group = new MarketDataIncrementalRefresh.NoMDEntries();
        group.set(new MDUpdateAction(MDUpdateAction.NEW));
        group.set(new Symbol("BTC-USD"));
        group.set(new MDEntryType(MDEntryType.BID));
        group.set(new MDEntryPx(50001.0));
        group.set(new MDEntrySize(0.5));
        message.addGroup(group);

        connector.fromApp(message, new SessionID("FIX.4.2", "SENDER", "TARGET"));

        verify(disruptorService).publish(eq("BTC-USD"), eq(50001_00000000L), eq(50000000L), eq("coinbase-fix"),
                eq(true));
    }

    private MarketDataSnapshotFullRefresh.NoMDEntries createGroup(char type, double px, double size) {
        MarketDataSnapshotFullRefresh.NoMDEntries group = new MarketDataSnapshotFullRefresh.NoMDEntries();
        group.set(new MDEntryType(type));
        group.set(new MDEntryPx(px));
        group.set(new MDEntrySize(size));
        return group;
    }
}

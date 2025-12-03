package com.hft.trading.fix;

import com.hft.trading.domain.Trade;
import org.springframework.stereotype.Service;
import quickfix.field.*;
import quickfix.fix44.MarketDataIncrementalRefresh;

@Service
public class MarketDataPublisher {

    public void publishTrade(Trade trade) {
        // Broadcast trade to all connected sessions (simplified)
        // In reality, we would filter by subscription.

        MarketDataIncrementalRefresh message = new MarketDataIncrementalRefresh();
        message.set(new MDReqID("AUTO"));

        MarketDataIncrementalRefresh.NoMDEntries group = new MarketDataIncrementalRefresh.NoMDEntries();
        group.set(new MDUpdateAction(MDUpdateAction.NEW));
        group.set(new MDEntryType(MDEntryType.TRADE));
        group.set(new MDEntryPx(trade.getPrice().doubleValue()));
        group.set(new MDEntrySize(trade.getQuantity().doubleValue()));
        group.set(new Symbol("SOL-USDC")); // Should come from trade

        message.addGroup(group);

        // Broadcast to all sessions (naive approach)
        // We need a way to get all active sessions.
        // QuickFIX/J doesn't expose "all sessions" easily without tracking them.
        // For this demo, we assume we track them in FixServerApplication or just send
        // to a known target if we had one.
        // Or we can iterate if we knew the SessionIDs.

        // For demonstration, we'll just print that we would send it.
        System.out.println("Publishing FIX Market Data: " + message);
    }
}

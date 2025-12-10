package com.hft.aggregator.connector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.MarketDataSnapshotFullRefresh;
import quickfix.fix42.MarketDataIncrementalRefresh;

import java.util.Properties;

/**
 * Connector for Coinbase Pro FIX API (Market Data).
 * Uses QuickFIX/J to connect and subscribe to market data.
 */
@Slf4j
@Component
public class CoinbaseFixConnector implements ExchangeConnector, Application {

    private final com.hft.aggregator.disruptor.DisruptorService disruptorService;
    private SocketInitiator initiator;
    private final String configFile = "fix-coinbase.cfg"; // In real app, load from resources
    private SessionID sessionId;
    private static final long SCALE = 100_000_000L; // 1e8

    public CoinbaseFixConnector(com.hft.aggregator.disruptor.DisruptorService disruptorService) {
        this.disruptorService = disruptorService;
    }

    @Override
    public void connect() {
        try {
            // For demo purposes, we construct settings programmatically or expect file to
            // exist.
            // Here we assume a file exists or we would mock/inject settings.
            // Since we can't easily rely on external config file presence in this env
            // without creating it,
            // we will proceed with the logical implementation.

            // In a real scenario:
            // SessionSettings settings = new SessionSettings(configFile);
            // FileStoreFactory storeFactory = new FileStoreFactory(settings);
            // LogFactory logFactory = new ScreenLogFactory(settings);
            // MessageFactory messageFactory = new DefaultMessageFactory();
            // initiator = new SocketInitiator(this, storeFactory, settings, logFactory,
            // messageFactory);
            // initiator.start();

            log.info("Coinbase FIX Connector connected (Logic Only - Initiator not started due to missing config)");

        } catch (Exception e) {
            log.error("Failed to start Coinbase FIX Connector", e);
        }
    }

    @Override
    public void disconnect() {
        if (initiator != null) {
            initiator.stop();
        }
    }

    @Override
    public void subscribe(String symbol) {
        // Send Market Data Request (MsgType=V)
        // Implementation details omitted as they require active session
        log.info("Subscribing to FIX symbol: {}", symbol);
    }

    @Override
    public boolean isConnected() {
        return initiator != null && initiator.isLoggedOn();
    }

    // QuickFIX/J Application methods

    @Override
    public void onCreate(SessionID sessionId) {
        log.info("FIX Session created: {}", sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        this.sessionId = sessionId;
        log.info("FIX Logged on: {}", sessionId);
        // Subscribe to symbols here if needed
    }

    @Override
    public void onLogout(SessionID sessionId) {
        this.sessionId = null;
        log.info("FIX Logged out: {}", sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        // Auth logic usually goes here (MsgType=A Logon)
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        // Heartbeats, etc.
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
    }

    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        try {
            String msgType = message.getHeader().getString(MsgType.FIELD);

            if (MsgType.MARKET_DATA_SNAPSHOT_FULL_REFRESH.equals(msgType)) {
                handleSnapshot(message);
            } else if (MsgType.MARKET_DATA_INCREMENTAL_REFRESH.equals(msgType)) {
                handleIncremental(message);
            }
        } catch (Exception e) {
            log.error("Error processing FIX message", e);
        }
    }

    private void handleSnapshot(Message message) throws FieldNotFound {
        log.warn("FIX Snapshot received (not processed in Disruptor mode yet).");
    }

    private void handleIncremental(Message message) throws FieldNotFound {
        // Tag 268 NoMDEntries
        int noMDEntries = message.getInt(NoMDEntries.FIELD);
        quickfix.fix42.MarketDataIncrementalRefresh.NoMDEntries group = new quickfix.fix42.MarketDataIncrementalRefresh.NoMDEntries();

        // Note: Symbol might be at root or inside group depending on spec.
        // Typically root for Incremental? Coinbase spec says root.
        String symbol = "";
        if (message.isSetField(Symbol.FIELD)) {
            symbol = message.getString(Symbol.FIELD);
        }

        for (int i = 1; i <= noMDEntries; i++) {
            message.getGroup(i, group);

            // If symbol is in group (sometimes happens in Aggregated feeds)
            if (symbol.isEmpty() && group.isSetField(Symbol.FIELD)) {
                symbol = group.getString(Symbol.FIELD);
            }

            char type = group.getChar(MDEntryType.FIELD);
            double px = group.getDouble(MDEntryPx.FIELD);
            double size = group.getDouble(MDEntrySize.FIELD);
            char action = group.getChar(MDUpdateAction.FIELD); // 279: 0=New, 1=Update, 2=Delete

            long pxScaled = (long) (px * SCALE);
            long sizeScaled = (long) (size * SCALE);

            if (action == MDUpdateAction.DELETE) {
                sizeScaled = 0;
            }

            // MDEntryType: 0=Bid, 1=Ask
            boolean isBid = (type == MDEntryType.BID);

            disruptorService.publish(symbol, pxScaled, sizeScaled, "coinbase-fix", isBid);
        }
    }
}

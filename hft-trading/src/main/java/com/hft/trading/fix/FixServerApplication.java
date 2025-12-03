package com.hft.trading.fix;

import org.springframework.stereotype.Component;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.MarketDataSnapshotFullRefresh;

@Component
public class FixServerApplication extends MessageCracker implements Application {

    @Override
    public void onCreate(SessionID sessionId) {
        System.out.println("Session created: " + sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("Logon: " + sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        System.out.println("Logout: " + sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
    }

    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionId);
    }

    public void onMessage(MarketDataRequest message, SessionID sessionId)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        // Handle Market Data Request
        // In a real app, we would register the subscription
        System.out.println("Received Market Data Request: " + message);

        // Send a dummy snapshot response for now to acknowledge
        MarketDataSnapshotFullRefresh snapshot = new MarketDataSnapshotFullRefresh();
        snapshot.set(new Symbol("SOL-USDC"));

        try {
            Session.sendToTarget(snapshot, sessionId);
        } catch (SessionNotFound e) {
            e.printStackTrace();
        }
    }
}

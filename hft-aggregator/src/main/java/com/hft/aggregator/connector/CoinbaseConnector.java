package com.hft.aggregator.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hft.aggregator.disruptor.DisruptorService;
import com.hft.aggregator.domain.OrderBookLevel;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Connector for Coinbase Pro WebSocket Feed.
 * Handles L2 order book updates and publishes to Disruptor.
 */
@Slf4j
@Component
public class CoinbaseConnector implements ExchangeConnector {

    private final DisruptorService disruptorService;
    private final ObjectMapper objectMapper;
    private final Map<String, String> symbolMapping = new ConcurrentHashMap<>(); // "BTC-USD" -> "BTC/USDT"
    private static final String COINBASE_WS_URL = "wss://ws-feed.exchange.coinbase.com";
    private static final long SCALE = 100_000_000L; // 1e8

    private WebSocketClient client;

    public CoinbaseConnector(DisruptorService disruptorService) {
        this.disruptorService = disruptorService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void connect() {
        if (client == null || client.isClosed()) {
            client = new WebSocketClient(URI.create(COINBASE_WS_URL)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    log.info("Coinbase WebSocket Connected");
                    subscribe("BTC-USD");
                }

                @Override
                public void onMessage(String message) {
                    CoinbaseConnector.this.onMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.warn("Coinbase WebSocket Closed: {} - {}", code, reason);
                }

                @Override
                public void onError(Exception ex) {
                    log.error("Coinbase WebSocket Error", ex);
                }
            };
            try {
                // Non-blocking connect usually preferred, but for simplicity/tests:
                client.connect();
            } catch (Exception e) {
                log.error("Error connecting to Coinbase", e);
            }
        }
    }

    @Override
    public void disconnect() {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public boolean isConnected() {
        return client != null && client.isOpen();
    }

    @Override
    public void subscribe(String symbol) {
        String coinbaseSymbol = symbol;
        String internalSymbol = symbol.replace("-", "/"); // BTC-USD -> BTC/USDT
        symbolMapping.put(coinbaseSymbol, internalSymbol);

        if (client != null && client.isOpen()) {
            sendSubscription(coinbaseSymbol);
        }
    }

    public void onMessage(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String type = node.path("type").asText();

            if ("snapshot".equals(type)) {
                handleSnapshot(node);
            } else if ("l2update".equals(type)) {
                handleL2Update(node);
            } else if ("error".equals(type)) {
                log.error("Coinbase Error: {}", node.path("message").asText());
            }

        } catch (Exception e) {
            log.error("Error processing Coinbase message", e);
        }
    }

    private void sendSubscription(String productId) {
        String subMessage = String.format(
                "{\"type\": \"subscribe\", \"product_ids\": [\"%s\"], \"channels\": [\"level2\"]}",
                productId);
        if (client != null && client.isOpen()) {
            client.send(subMessage);
            log.info("Subscribed to Coinbase product: {}", productId);
        }
    }

    private void handleSnapshot(JsonNode node) {
        log.warn("Snapshot received (not processed in Disruptor mode yet).");
    }

    private void handleL2Update(JsonNode node) {
        String productId = node.path("product_id").asText();
        String internalSymbol = symbolMapping.getOrDefault(productId, productId);
        JsonNode changes = node.path("changes");

        for (JsonNode change : changes) {
            String side = change.get(0).asText(); // "buy" or "sell"
            double price = change.get(1).asDouble();
            double size = change.get(2).asDouble();

            long priceScaled = (long) (price * SCALE);
            long sizeScaled = (long) (size * SCALE);

            boolean isBid = "buy".equals(side);
            disruptorService.publish(internalSymbol, priceScaled, sizeScaled, "coinbase", isBid);
        }
    }
}

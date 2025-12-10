package com.hft.aggregator.connector;

/**
 * Standard interface for exchange connectors.
 */
public interface ExchangeConnector {

    /**
     * Connect to the exchange.
     */
    void connect();

    /**
     * Disconnect from the exchange.
     */
    void disconnect();

    /**
     * Subscribe to market data for a specific symbol.
     * 
     * @param symbol Pair symbol (e.g., "BTC-USD")
     */
    void subscribe(String symbol);

    /**
     * Check if connected.
     */
    boolean isConnected();
}

package com.hft.aggregator.domain;

import org.agrona.collections.Object2IntHashMap;

/**
 * Maps String symbols to int IDs for zero-allocation lookup.
 * Single-threaded writer (at startup or via Disruptor), Multi-threaded reader
 * (volatile reads).
 */
public class SymbolDictionary {
    private static final int MAX_SYMBOLS = 4096;
    private static final String[] symbolById = new String[MAX_SYMBOLS];
    private static final Object2IntHashMap<String> idBySymbol = new Object2IntHashMap<>(-1);
    private static int nextId = 0;

    /**
     * Get or create ID for symbol.
     * Not thread-safe for concurrent writes - intended to be called during init or
     * by single writer.
     */
    public static int getOrCreateId(String symbol) {
        int id = idBySymbol.getValue(symbol);
        if (id == -1) {
            id = nextId++;
            if (id >= MAX_SYMBOLS) {
                throw new IllegalStateException("Max symbols limit reached");
            }
            symbolById[id] = symbol;
            idBySymbol.put(symbol, id);
        }
        return id;
    }

    public static String getSymbol(int id) {
        if (id < 0 || id >= nextId) {
            return null;
        }
        return symbolById[id];
    }

    public static int getId(String symbol) {
        return idBySymbol.getValue(symbol);
    }
}

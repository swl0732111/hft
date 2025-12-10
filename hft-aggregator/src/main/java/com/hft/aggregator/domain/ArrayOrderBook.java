package com.hft.aggregator.domain;

import lombok.Getter;

import java.util.Arrays;

/**
 * Flat array-backed order book for Zero GC and high cache locality.
 * Single Writer (Disruptor), Multiple Readers.
 */
public class ArrayOrderBook {
    @Getter
    private final int symbolId;
    private final int maxDepth;

    // Flat Arrays: [Index 0 = Best Price]
    // Parallel arrays for Price and Quantity
    private final long[] bidPrices;
    private final long[] bidQuantities;
    private final int[] bidSourceIds; // Track source per level

    public long getSpread() {
        if (bidCount == 0 || askCount == 0)
            return 0;
        return askPrices[0] - bidPrices[0];
    }

    public double getMidPrice() {
        if (bidCount == 0 || askCount == 0)
            return 0.0;
        return (askPrices[0] + bidPrices[0]) / 2.0;
    }

    public double getAvailableLiquidity(boolean isBuy, double maxQuantity) {
        // isBuy=true -> Buying -> consume Asks
        int count = isBuy ? askCount : bidCount;
        long[] quantities = isBuy ? askQuantities : bidQuantities;

        long totalScaled = 0;
        long maxScaled = (long) (maxQuantity * 100_000_000L); // Assuming 1e8 scale

        for (int i = 0; i < count; i++) {
            totalScaled += quantities[i];
            if (totalScaled >= maxScaled)
                return maxQuantity;
        }
        return totalScaled / 100_000_000.0;
    }

    public int getBidCount() {
        return bidCount;
    }

    public int getAskCount() {
        return askCount;
    }

    public long getBidPrice(int index) {
        return bidPrices[index];
    }

    public long getBidQuantity(int index) {
        return bidQuantities[index];
    }

    public long getAskPrice(int index) {
        return askPrices[index];
    }

    public long getAskQuantity(int index) {
        return askQuantities[index];
    }

    private final long[] askPrices;
    private final long[] askQuantities;
    private final int[] askSourceIds;

    private int bidCount = 0;

    private int askCount = 0;

    @Getter
    private long lastUpdateNanos;

    public ArrayOrderBook(int symbolId, int maxDepth) {
        this.symbolId = symbolId;
        this.maxDepth = maxDepth;

        // Allocate with padding to avoid false sharing if needed, but simple arrays for
        // now
        this.bidPrices = new long[maxDepth];
        this.bidQuantities = new long[maxDepth];
        this.bidSourceIds = new int[maxDepth];
        this.askPrices = new long[maxDepth];
        this.askQuantities = new long[maxDepth];
        this.askSourceIds = new int[maxDepth];
    }

    public void updateBid(long price, long quantity, int sourceId) {
        lastUpdateNanos = System.nanoTime();
        // Linear scan is faster than binary search for small depths (e.g., 20)
        // Bids are sorted DESCENDING (Higher is better)

        int pos = -1;
        // Find position or insertion point
        for (int i = 0; i < bidCount; i++) {
            if (bidPrices[i] == price) {
                if (quantity == 0) {
                    removeBid(i);
                } else {
                    bidQuantities[i] = quantity; // Update existing
                    bidSourceIds[i] = sourceId;
                }
                return;
            }
            if (bidPrices[i] < price) {
                pos = i;
                break;
            }
        }

        // Make insertion if new level and non-zero qty
        if (quantity > 0) {
            if (pos == -1) {
                // Must be at end
                if (bidCount < maxDepth) {
                    insertBidAt(bidCount, price, quantity, sourceId);
                }
            } else {
                insertBidAt(pos, price, quantity, sourceId);
            }
        }
    }

    private void insertBidAt(int index, long price, long quantity, int sourceId) {
        if (bidCount < maxDepth) {
            // Shift down
            System.arraycopy(bidPrices, index, bidPrices, index + 1, bidCount - index);
            System.arraycopy(bidQuantities, index, bidQuantities, index + 1, bidCount - index);
            System.arraycopy(bidSourceIds, index, bidSourceIds, index + 1, bidCount - index);
            bidCount++;
        } else {
            // Shift down but drop last
            System.arraycopy(bidPrices, index, bidPrices, index + 1, maxDepth - 1 - index);
            System.arraycopy(bidQuantities, index, bidQuantities, index + 1, maxDepth - 1 - index);
            System.arraycopy(bidSourceIds, index, bidSourceIds, index + 1, maxDepth - 1 - index);
        }
        bidPrices[index] = price;
        bidQuantities[index] = quantity;
        bidSourceIds[index] = sourceId;
    }

    private void removeBid(int index) {
        int numMoved = bidCount - index - 1;
        if (numMoved > 0) {
            System.arraycopy(bidPrices, index + 1, bidPrices, index, numMoved);
            System.arraycopy(bidQuantities, index + 1, bidQuantities, index, numMoved);
            System.arraycopy(bidSourceIds, index + 1, bidSourceIds, index, numMoved);
        }
        bidCount--;
        // Clear last (optional for primitives but good for debug)
        bidPrices[bidCount] = 0;
        bidQuantities[bidCount] = 0;
        bidSourceIds[bidCount] = 0;
    }

    public void updateAsk(long price, long quantity, int sourceId) {
        lastUpdateNanos = System.nanoTime();
        // Asks are sorted ASCENDING (Lower is better)

        int pos = -1;
        for (int i = 0; i < askCount; i++) {
            if (askPrices[i] == price) {
                if (quantity == 0) {
                    removeAsk(i);
                } else {
                    askQuantities[i] = quantity;
                    askSourceIds[i] = sourceId;
                }
                return;
            }
            if (askPrices[i] > price) {
                pos = i;
                break;
            }
        }

        if (quantity > 0) {
            if (pos == -1) {
                if (askCount < maxDepth) {
                    insertAskAt(askCount, price, quantity, sourceId);
                }
            } else {
                insertAskAt(pos, price, quantity, sourceId);
            }
        }
    }

    private void insertAskAt(int index, long price, long quantity, int sourceId) {
        if (askCount < maxDepth) {
            System.arraycopy(askPrices, index, askPrices, index + 1, askCount - index);
            System.arraycopy(askQuantities, index, askQuantities, index + 1, askCount - index);
            System.arraycopy(askSourceIds, index, askSourceIds, index + 1, askCount - index);
            askCount++;
        } else {
            System.arraycopy(askPrices, index, askPrices, index + 1, maxDepth - 1 - index);
            System.arraycopy(askQuantities, index, askQuantities, index + 1, maxDepth - 1 - index);
            System.arraycopy(askSourceIds, index, askSourceIds, index + 1, maxDepth - 1 - index);
        }
        askPrices[index] = price;
        askQuantities[index] = quantity;
        askSourceIds[index] = sourceId;
    }

    private void removeAsk(int index) {
        int numMoved = askCount - index - 1;
        if (numMoved > 0) {
            System.arraycopy(askPrices, index + 1, askPrices, index, numMoved);
            System.arraycopy(askQuantities, index + 1, askQuantities, index, numMoved);
            System.arraycopy(askSourceIds, index + 1, askSourceIds, index, numMoved);
        }
        askCount--;
        askPrices[askCount] = 0;
        askQuantities[askCount] = 0;
        askSourceIds[askCount] = 0;
    }

    public long getBestBidPrice() {
        return bidCount > 0 ? bidPrices[0] : 0;
    }

    public long getBestBidQuantity() {
        return bidCount > 0 ? bidQuantities[0] : 0;
    }

    public long getBestAskPrice() {
        return askCount > 0 ? askPrices[0] : 0;
    }

    public long getBestAskQuantity() {
        return askCount > 0 ? askQuantities[0] : 0;
    }

    public int getBidSourceId(int index) {
        return bidSourceIds[index];
    }

    public int getAskSourceId(int index) {
        return askSourceIds[index];
    }
}

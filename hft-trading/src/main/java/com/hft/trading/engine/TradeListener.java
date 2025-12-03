package com.hft.trading.engine;

/**
 * Callback interface for trade events.
 * Eliminates Trade object allocations by using primitive parameters.
 */
@FunctionalInterface
public interface TradeListener {
    /**
     * Called when a trade is executed.
     * 
     * @param makerOrderId   ID of the maker order
     * @param takerOrderId   ID of the taker order
     * @param priceScaled    Price in scaled long format (8 decimal places)
     * @param quantityScaled Quantity in scaled long format (8 decimal places)
     * @param timestamp      Trade execution timestamp
     */
    void onTrade(String makerOrderId, String takerOrderId, long priceScaled, long quantityScaled, long timestamp);
}

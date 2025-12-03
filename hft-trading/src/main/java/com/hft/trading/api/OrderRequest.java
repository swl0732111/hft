package com.hft.trading.api;

import com.hft.trading.domain.Order;
import lombok.Data;

@Data
public class OrderRequest {
    private String walletAddress;
    private String symbol;
    private Order.Side side;
    private Order.Chain chain;
    private Double price;
    private Double quantity;
    private String accountId;
    private Order.OrderType orderType;

    // Optional fields for advanced order types
    private Double stopPrice;
    private Double displayQuantity;
}

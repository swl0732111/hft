package com.hft.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TradeHistoryDTO {
    private String transactionId;
    private String orderId;
    private String symbol;
    private String type; // BUY/SELL/DEPOSIT/WITHDRAW
    private double amount;
    private double price;
    private String status;
    private long timestamp;
    private String txHash; // For blockchain transactions
    private String asset; // For deposits/withdrawals
}

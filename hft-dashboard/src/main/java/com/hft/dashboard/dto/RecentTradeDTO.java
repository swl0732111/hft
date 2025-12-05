package com.hft.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for recent trade information. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentTradeDTO {
  private String symbol;
  private String side; // BUY or SELL
  private double price;
  private double quantity;
  private double feePaid;
  private long timestamp;
  private String orderId;
}

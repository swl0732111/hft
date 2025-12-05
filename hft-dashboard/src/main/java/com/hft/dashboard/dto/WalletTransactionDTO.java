package com.hft.dashboard.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WalletTransactionDTO {
  private String transactionId;
  private String type; // DEPOSIT, WITHDRAWAL
  private String asset; // USDT, BTC, etc.
  private BigDecimal amount;
  private String status; // COMPLETED, PENDING, FAILED
  private long timestamp;
  private String txHash; // Blockchain transaction hash (mock)
}

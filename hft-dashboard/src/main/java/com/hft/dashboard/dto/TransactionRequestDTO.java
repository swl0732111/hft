package com.hft.dashboard.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class TransactionRequestDTO {
  private String accountId;
  private String type;
  private String asset;
  private BigDecimal amount;
  private String walletAddress;
  private String signature;
  private String nonce;
}

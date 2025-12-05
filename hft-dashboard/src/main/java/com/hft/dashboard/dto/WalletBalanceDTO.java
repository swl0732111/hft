package com.hft.dashboard.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WalletBalanceDTO {
  private String asset;
  private BigDecimal availableBalance;
  private BigDecimal lockedBalance;
  private BigDecimal totalBalance;
  private BigDecimal usdValue;
}

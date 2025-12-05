package com.hft.dashboard.dto;

import com.hft.common.domain.UserTier;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for user tier information and progress. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierInfoDTO {
  private UserTier currentTier;
  private int tierLevel;
  private double volume30d;
  private UserTier nextTier;
  private double volumeToNextTier;
  private double progressPercent;
  private int makerFeeBps;
  private int takerFeeBps;
  private double makerFeePercent;
  private double takerFeePercent;
  private double feeSavings; // Compared to VIP0
  private long lastTierUpdate;
  private boolean tierLocked;
  private Long tierLockedUntil;
  private TierBenefitsDTO benefits;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TierBenefitsDTO {
    private String apiRateLimit;
    private String supportPriority;
    private boolean priorityWithdrawal;
    private boolean dedicatedAccountManager;
    private boolean customApiSolutions;
    private Map<String, Object> additionalBenefits;
  }
}

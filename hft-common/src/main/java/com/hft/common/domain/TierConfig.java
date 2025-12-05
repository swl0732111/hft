package com.hft.common.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Configuration for user tier benefits and requirements. Allows dynamic configuration of tier
 * thresholds and benefits.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("tier_config")
public class TierConfig {
  @Id private String id;

  private UserTier tier;

  // Volume requirements (scaled long format)
  private long minVolumeScaled;
  private long maxVolumeScaled;

  // Fee rates in basis points
  private int makerFeeBps;
  private int takerFeeBps;

  // API rate limits (requests per second)
  private int apiRateLimitRps;

  // Support priority (1 = highest, 5 = lowest)
  private int supportPriority;

  // Additional benefits
  private boolean dedicatedAccountManager;
  private boolean priorityWithdrawal;
  private boolean customApiSolutions;

  // Metadata
  private String description;
  private boolean active;
  private long createdAt;
  private long updatedAt;

  /** Create default tier config from UserTier enum. */
  public static TierConfig fromUserTier(UserTier tier) {
    return TierConfig.builder()
        .tier(tier)
        .minVolumeScaled(tier.getMinVolumeScaled())
        .maxVolumeScaled(tier.getMaxVolumeScaled())
        .makerFeeBps(tier.getMakerFeeBps())
        .takerFeeBps(tier.getTakerFeeBps())
        .apiRateLimitRps(getDefaultApiRateLimit(tier))
        .supportPriority(getDefaultSupportPriority(tier))
        .dedicatedAccountManager(tier.getLevel() >= UserTier.VIP4.getLevel())
        .priorityWithdrawal(tier.getLevel() >= UserTier.VIP3.getLevel())
        .customApiSolutions(tier == UserTier.VIP5)
        .description(tier.getDescription())
        .active(true)
        .createdAt(System.currentTimeMillis())
        .updatedAt(System.currentTimeMillis())
        .build();
  }

  private static int getDefaultApiRateLimit(UserTier tier) {
    switch (tier) {
      case VIP0:
        return 10; // 10 req/s
      case VIP1:
        return 20; // 20 req/s
      case VIP2:
        return 50; // 50 req/s
      case VIP3:
        return 100; // 100 req/s
      case VIP4:
        return 200; // 200 req/s
      case VIP5:
        return 500; // 500 req/s
      default:
        return 10;
    }
  }

  private static int getDefaultSupportPriority(UserTier tier) {
    // Lower number = higher priority
    return 6 - tier.getLevel(); // VIP0=6, VIP1=5, ..., VIP5=1
  }

  /** Check if volume qualifies for this tier. */
  public boolean qualifiesForTier(long volume30dScaled) {
    return volume30dScaled >= minVolumeScaled && volume30dScaled < maxVolumeScaled;
  }
}

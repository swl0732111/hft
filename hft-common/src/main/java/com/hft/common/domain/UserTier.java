package com.hft.common.domain;

import com.hft.common.util.FixedPointMath;

/**
 * User VIP tier levels based on 30-day trading volume. Higher tiers receive lower trading fees and
 * additional benefits.
 */
public enum UserTier {
  /** VIP0 - Default tier for new users Volume: < $100,000 Maker: 0.10% / Taker: 0.20% */
  VIP0(0, 0L, 100_000_00000000L, 10, 20),

  /** VIP1 - Bronze tier Volume: $100,000 - $500,000 Maker: 0.08% / Taker: 0.18% */
  VIP1(1, 100_000_00000000L, 500_000_00000000L, 8, 18),

  /**
   * VIP2 - Silver tier Volume: $500,000 - $2,000,000 Maker: 0.06% / Taker: 0.15% Benefits: Priority
   * customer support
   */
  VIP2(2, 500_000_00000000L, 2_000_000_00000000L, 6, 15),

  /**
   * VIP3 - Gold tier Volume: $2,000,000 - $10,000,000 Maker: 0.04% / Taker: 0.12% Benefits: API
   * rate limit increase
   */
  VIP3(3, 2_000_000_00000000L, 10_000_000_00000000L, 4, 12),

  /**
   * VIP4 - Platinum tier Volume: $10,000,000 - $50,000,000 Maker: 0.02% / Taker: 0.08% Benefits:
   * Dedicated account manager
   */
  VIP4(4, 10_000_000_00000000L, 50_000_000_00000000L, 2, 8),

  /**
   * VIP5 - Diamond tier Volume: > $50,000,000 Maker: 0.00% / Taker: 0.05% Benefits: Zero maker
   * fees, custom solutions
   */
  VIP5(5, 50_000_000_00000000L, Long.MAX_VALUE, 0, 5);

  private final int level;
  private final long minVolumeScaled; // Minimum 30-day volume (scaled)
  private final long maxVolumeScaled; // Maximum 30-day volume (scaled)
  private final int makerFeeBps; // Maker fee in basis points
  private final int takerFeeBps; // Taker fee in basis points

  UserTier(
      int level, long minVolumeScaled, long maxVolumeScaled, int makerFeeBps, int takerFeeBps) {
    this.level = level;
    this.minVolumeScaled = minVolumeScaled;
    this.maxVolumeScaled = maxVolumeScaled;
    this.makerFeeBps = makerFeeBps;
    this.takerFeeBps = takerFeeBps;
  }

  /**
   * Determine tier from 30-day trading volume.
   *
   * @param volume30dScaled 30-day trading volume in scaled format
   * @return Appropriate tier for the volume
   */
  public static UserTier fromVolume(long volume30dScaled) {
    for (UserTier tier : values()) {
      if (volume30dScaled >= tier.minVolumeScaled && volume30dScaled < tier.maxVolumeScaled) {
        return tier;
      }
    }
    return VIP0; // Default to VIP0 if no match
  }

  public int getLevel() {
    return level;
  }

  public long getMinVolumeScaled() {
    return minVolumeScaled;
  }

  public long getMaxVolumeScaled() {
    return maxVolumeScaled;
  }

  public int getMakerFeeBps() {
    return makerFeeBps;
  }

  public int getTakerFeeBps() {
    return takerFeeBps;
  }

  /** Get minimum volume in human-readable format. */
  public double getMinVolume() {
    return FixedPointMath.toDouble(minVolumeScaled);
  }

  /** Get maximum volume in human-readable format. */
  public double getMaxVolume() {
    return maxVolumeScaled == Long.MAX_VALUE
        ? Double.POSITIVE_INFINITY
        : FixedPointMath.toDouble(maxVolumeScaled);
  }

  /** Get maker fee rate as percentage. */
  public double getMakerFeePercent() {
    return makerFeeBps / 100.0;
  }

  /** Get taker fee rate as percentage. */
  public double getTakerFeePercent() {
    return takerFeeBps / 100.0;
  }

  /**
   * Get next tier level.
   *
   * @return Next tier, or null if already at max tier
   */
  public UserTier getNextTier() {
    return level < VIP5.level ? values()[level + 1] : null;
  }

  /**
   * Get previous tier level.
   *
   * @return Previous tier, or null if already at min tier
   */
  public UserTier getPreviousTier() {
    return level > VIP0.level ? values()[level - 1] : null;
  }

  /** Check if this tier has zero maker fees. */
  public boolean hasZeroMakerFee() {
    return makerFeeBps == 0;
  }

  /**
   * Get volume needed to reach next tier.
   *
   * @param currentVolumeScaled Current 30-day volume
   * @return Volume needed, or 0 if already at max tier
   */
  public long getVolumeToNextTier(long currentVolumeScaled) {
    UserTier nextTier = getNextTier();
    if (nextTier == null) {
      return 0;
    }
    return Math.max(0, nextTier.minVolumeScaled - currentVolumeScaled);
  }

  /** Get tier description with benefits. */
  public String getDescription() {
    switch (this) {
      case VIP0:
        return "Default tier for all users";
      case VIP1:
        return "Bronze tier - Reduced fees";
      case VIP2:
        return "Silver tier - Priority support";
      case VIP3:
        return "Gold tier - Higher API limits";
      case VIP4:
        return "Platinum tier - Dedicated account manager";
      case VIP5:
        return "Diamond tier - Zero maker fees, custom solutions";
      default:
        return "";
    }
  }
}

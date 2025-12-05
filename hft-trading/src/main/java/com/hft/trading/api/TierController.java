package com.hft.trading.api;

import com.hft.common.domain.Account;
import com.hft.common.domain.TierConfig;
import com.hft.common.domain.UserTier;
import com.hft.common.util.FixedPointMath;
import com.hft.trading.repository.AccountRepository;
import com.hft.trading.service.TierService;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST API for user tier information and management. */
@RestController
@RequestMapping("/api/tiers")
@RequiredArgsConstructor
public class TierController {
  private final TierService tierService;
  private final AccountRepository accountRepository;

  /** Get current user tier information. */
  @GetMapping("/current")
  public ResponseEntity<TierInfoResponse> getCurrentTier(@RequestParam String accountId) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));

    UserTier currentTier = account.getCurrentTierOrDefault();
    TierConfig tierConfig = tierService.getTierConfig(currentTier);

    return ResponseEntity.ok(
        TierInfoResponse.builder()
            .tier(currentTier)
            .tierLevel(currentTier.getLevel())
            .tierName(currentTier.name())
            .description(currentTier.getDescription())
            .makerFeeBps(tierConfig.getMakerFeeBps())
            .takerFeeBps(tierConfig.getTakerFeeBps())
            .makerFeePercent(currentTier.getMakerFeePercent())
            .takerFeePercent(currentTier.getTakerFeePercent())
            .volume30d(FixedPointMath.toDouble(account.getVolume30dScaled()))
            .apiRateLimitRps(tierConfig.getApiRateLimitRps())
            .supportPriority(tierConfig.getSupportPriority())
            .dedicatedAccountManager(tierConfig.isDedicatedAccountManager())
            .tierLocked(account.isTierLocked())
            .lastTierUpdate(account.getLastTierUpdate())
            .build());
  }

  /** Get all available tier benefits. */
  @GetMapping("/benefits")
  public ResponseEntity<List<TierBenefitsResponse>> getAllTierBenefits() {
    List<TierBenefitsResponse> benefits =
        Arrays.stream(UserTier.values())
            .map(
                tier -> {
                  TierConfig config = tierService.getTierConfig(tier);
                  return TierBenefitsResponse.builder()
                      .tier(tier)
                      .tierLevel(tier.getLevel())
                      .tierName(tier.name())
                      .description(tier.getDescription())
                      .minVolume(tier.getMinVolume())
                      .maxVolume(tier.getMaxVolume())
                      .makerFeeBps(config.getMakerFeeBps())
                      .takerFeeBps(config.getTakerFeeBps())
                      .makerFeePercent(tier.getMakerFeePercent())
                      .takerFeePercent(tier.getTakerFeePercent())
                      .apiRateLimitRps(config.getApiRateLimitRps())
                      .supportPriority(config.getSupportPriority())
                      .dedicatedAccountManager(config.isDedicatedAccountManager())
                      .priorityWithdrawal(config.isPriorityWithdrawal())
                      .customApiSolutions(config.isCustomApiSolutions())
                      .build();
                })
            .collect(Collectors.toList());

    return ResponseEntity.ok(benefits);
  }

  /** Get 30-day volume statistics. */
  @GetMapping("/volume")
  public ResponseEntity<VolumeStatsResponse> getVolumeStats(@RequestParam String accountId) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));

    long volume30d = tierService.calculate30DayVolume(accountId);
    UserTier currentTier = account.getCurrentTierOrDefault();

    return ResponseEntity.ok(
        VolumeStatsResponse.builder()
            .accountId(accountId)
            .volume30d(FixedPointMath.toDouble(volume30d))
            .currentTier(currentTier)
            .lastUpdate(account.getLastTierUpdate())
            .build());
  }

  /** Get progress to next tier. */
  @GetMapping("/progress")
  public ResponseEntity<TierProgressResponse> getTierProgress(@RequestParam String accountId) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));

    UserTier currentTier = account.getCurrentTierOrDefault();
    UserTier nextTier = currentTier.getNextTier();
    long currentVolume = account.getVolume30dScaled();
    long volumeToNextTier = tierService.getVolumeToNextTier(accountId);

    double progressPercent = 0.0;
    if (nextTier != null && volumeToNextTier > 0) {
      long tierRange = nextTier.getMinVolumeScaled() - currentTier.getMinVolumeScaled();
      long volumeInTier = currentVolume - currentTier.getMinVolumeScaled();
      progressPercent = (double) volumeInTier / tierRange * 100.0;
    }

    return ResponseEntity.ok(
        TierProgressResponse.builder()
            .currentTier(currentTier)
            .nextTier(nextTier)
            .currentVolume(FixedPointMath.toDouble(currentVolume))
            .volumeToNextTier(FixedPointMath.toDouble(volumeToNextTier))
            .progressPercent(Math.min(100.0, Math.max(0.0, progressPercent)))
            .atMaxTier(nextTier == null)
            .build());
  }

  /** Manually set tier (admin only). */
  @PostMapping("/admin/set-tier")
  public ResponseEntity<String> setTierManually(
      @RequestParam String accountId,
      @RequestParam UserTier tier,
      @RequestParam(required = false) Long lockUntilMillis) {

    long lockUntil =
        lockUntilMillis != null
            ? lockUntilMillis
            : System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000); // 30 days default

    tierService.setTierManually(accountId, tier, lockUntil);
    return ResponseEntity.ok("Tier set to " + tier + " for account " + accountId);
  }

  /** Unlock tier (admin only). */
  @PostMapping("/admin/unlock-tier")
  public ResponseEntity<String> unlockTier(@RequestParam String accountId) {
    tierService.unlockTier(accountId);
    return ResponseEntity.ok("Tier unlocked for account " + accountId);
  }

  /** Force tier recalculation (admin only). */
  @PostMapping("/admin/recalculate")
  public ResponseEntity<String> recalculateTier(@RequestParam String accountId) {
    boolean updated = tierService.updateAccountTier(accountId);
    return ResponseEntity.ok(updated ? "Tier updated" : "Tier unchanged");
  }

  // Response DTOs

  @Data
  @Builder
  @AllArgsConstructor
  public static class TierInfoResponse {
    private UserTier tier;
    private int tierLevel;
    private String tierName;
    private String description;
    private int makerFeeBps;
    private int takerFeeBps;
    private double makerFeePercent;
    private double takerFeePercent;
    private double volume30d;
    private int apiRateLimitRps;
    private int supportPriority;
    private boolean dedicatedAccountManager;
    private boolean tierLocked;
    private long lastTierUpdate;
  }

  @Data
  @Builder
  @AllArgsConstructor
  public static class TierBenefitsResponse {
    private UserTier tier;
    private int tierLevel;
    private String tierName;
    private String description;
    private double minVolume;
    private double maxVolume;
    private int makerFeeBps;
    private int takerFeeBps;
    private double makerFeePercent;
    private double takerFeePercent;
    private int apiRateLimitRps;
    private int supportPriority;
    private boolean dedicatedAccountManager;
    private boolean priorityWithdrawal;
    private boolean customApiSolutions;
  }

  @Data
  @Builder
  @AllArgsConstructor
  public static class VolumeStatsResponse {
    private String accountId;
    private double volume30d;
    private UserTier currentTier;
    private long lastUpdate;
  }

  @Data
  @Builder
  @AllArgsConstructor
  public static class TierProgressResponse {
    private UserTier currentTier;
    private UserTier nextTier;
    private double currentVolume;
    private double volumeToNextTier;
    private double progressPercent;
    private boolean atMaxTier;
  }
}

package com.hft.trading.service;

import com.hft.common.domain.Account;
import com.hft.common.domain.TierConfig;
import com.hft.common.domain.UserTier;
import com.hft.account.repository.AccountRepository;
import com.hft.trading.repository.TierConfigRepository;
import com.hft.trading.repository.TradingVolumeStatsRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user tier calculations and upgrades. Calculates tiers
 * based on 30-day
 * rolling trading volume.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TierService {
  private final AccountRepository accountRepository;
  private final TradingVolumeStatsRepository volumeStatsRepository;
  private final TierConfigRepository tierConfigRepository;

  // Cache for tier configs
  private final Map<UserTier, TierConfig> tierConfigCache = new ConcurrentHashMap<>();

  /** Get tier configuration for a specific tier level. */
  public TierConfig getTierConfig(UserTier tier) {
    return tierConfigCache.computeIfAbsent(
        tier,
        t -> {
          return tierConfigRepository
              .findByTierAndActive(t, true)
              .orElseGet(() -> TierConfig.fromUserTier(t));
        });
  }

  /**
   * Calculate 30-day trading volume for an account.
   *
   * @param accountId Account ID
   * @return Total volume in scaled format
   */
  public long calculate30DayVolume(String accountId) {
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusDays(30);

    return volumeStatsRepository.sumVolumeByAccountIdAndDateBetween(accountId, startDate, endDate);
  }

  /**
   * Calculate appropriate tier based on 30-day volume.
   *
   * @param accountId Account ID
   * @return Calculated tier
   */
  public UserTier calculateTier(String accountId) {
    long volume30d = calculate30DayVolume(accountId);
    return UserTier.fromVolume(volume30d);
  }

  /**
   * Update account tier if it has changed. Respects tier locks (e.g., promotional
   * tiers).
   *
   * @param accountId Account ID
   * @return True if tier was updated
   */
  @Transactional
  public boolean updateAccountTier(String accountId) {
    Account account = accountRepository
        .findById(accountId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

    // Don't update if tier is locked (promotional tier)
    if (account.isTierLocked()) {
      log.debug("Tier locked for account {}, skipping update", accountId);
      return false;
    }

    long volume30d = calculate30DayVolume(accountId);
    UserTier newTier = UserTier.fromVolume(volume30d);
    UserTier currentTier = account.getCurrentTierOrDefault();

    if (newTier != currentTier) {
      UserTier oldTier = currentTier;
      account.setCurrentTier(newTier);
      account.setVolume30dScaled(volume30d);
      account.setLastTierUpdate(System.currentTimeMillis());
      accountRepository.save(account);

      log.info(
          "Tier updated for account {}: {} -> {} (volume: {})",
          accountId,
          oldTier,
          newTier,
          volume30d);
      return true;
    }

    // Update volume even if tier didn't change
    account.setVolume30dScaled(volume30d);
    account.setLastTierUpdate(System.currentTimeMillis());
    accountRepository.save(account);

    return false;
  }

  /**
   * Get progress to next tier.
   *
   * @param accountId Account ID
   * @return Volume needed to reach next tier (scaled), or 0 if at max tier
   */
  public long getVolumeToNextTier(String accountId) {
    Account account = accountRepository
        .findById(accountId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

    UserTier currentTier = account.getCurrentTierOrDefault();
    long currentVolume = account.getVolume30dScaled();

    return currentTier.getVolumeToNextTier(currentVolume);
  }

  /**
   * Manually set tier for an account (admin function). Locks tier until specified
   * timestamp.
   *
   * @param accountId       Account ID
   * @param tier            Tier to set
   * @param lockUntilMillis Timestamp until which tier is locked
   */
  @Transactional
  public void setTierManually(String accountId, UserTier tier, long lockUntilMillis) {
    Account account = accountRepository
        .findById(accountId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

    account.setCurrentTier(tier);
    account.setTierLockedUntil(lockUntilMillis);
    account.setLastTierUpdate(System.currentTimeMillis());
    accountRepository.save(account);

    log.info(
        "Tier manually set for account {}: {} (locked until {})", accountId, tier, lockUntilMillis);
  }

  /**
   * Unlock tier for an account (admin function). Allows automatic tier
   * calculation to resume.
   *
   * @param accountId Account ID
   */
  @Transactional
  public void unlockTier(String accountId) {
    Account account = accountRepository
        .findById(accountId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

    account.setTierLockedUntil(null);
    accountRepository.save(account);

    log.info("Tier unlocked for account {}", accountId);

    // Immediately recalculate tier
    updateAccountTier(accountId);
  }

  /**
   * Scheduled job to recalculate all account tiers daily. Runs at 2 AM every day.
   */
  @Scheduled(cron = "0 0 2 * * *")
  @Transactional
  public void recalculateAllTiers() {
    log.info("Starting daily tier recalculation");
    long startTime = System.currentTimeMillis();
    int updatedCount = 0;
    int totalCount = 0;

    Iterable<Account> accounts = accountRepository.findAll();
    for (Account account : accounts) {
      if (account.getStatus() == Account.AccountStatus.ACTIVE) {
        totalCount++;
        if (updateAccountTier(account.getId())) {
          updatedCount++;
        }
      }
    }

    long duration = System.currentTimeMillis() - startTime;
    log.info(
        "Tier recalculation completed: {} accounts processed, {} tiers updated, duration: {}ms",
        totalCount,
        updatedCount,
        duration);
  }

  /** Reload tier config cache from database. */
  public void reloadTierConfigs() {
    tierConfigCache.clear();
    tierConfigRepository
        .findByActive(true)
        .forEach(
            config -> {
              tierConfigCache.put(config.getTier(), config);
            });
    log.info("Tier configs reloaded: {} tiers", tierConfigCache.size());
  }

  /** Initialize default tier configurations if not present. */
  @Transactional
  public void initializeDefaultTierConfigs() {
    for (UserTier tier : UserTier.values()) {
      if (tierConfigRepository.findByTierAndActive(tier, true).isEmpty()) {
        TierConfig config = TierConfig.fromUserTier(tier);
        tierConfigRepository.save(config);
        log.info("Created default tier config for {}", tier);
      }
    }
    reloadTierConfigs();
  }
}

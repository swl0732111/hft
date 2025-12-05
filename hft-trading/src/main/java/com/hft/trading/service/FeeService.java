package com.hft.trading.service;

import com.hft.common.domain.Account;
import com.hft.common.domain.UserTier;
import com.hft.trading.domain.FeeConfig;
import com.hft.trading.domain.TieredFeeConfig;
import com.hft.trading.repository.AccountRepository;
import com.hft.trading.repository.FeeConfigRepository;
import com.hft.trading.repository.TieredFeeConfigRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fee calculation and deduction service. Supports Maker/Taker fee model with tiered rates based on
 * user VIP level.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeService {
    private final FeeConfigRepository feeConfigRepository;
  private final TieredFeeConfigRepository tieredFeeConfigRepository;
  private final AccountRepository accountRepository;
    private final AccountService accountService;

  // Cache for legacy fee configs (reload periodically)
  private final Map<String, FeeConfig> feeConfigCache = new ConcurrentHashMap<>();

  // Cache for tiered fee configs: "symbol:tier" -> TieredFeeConfig
  private final Map<String, TieredFeeConfig> tieredFeeConfigCache = new ConcurrentHashMap<>();

  /** Get tiered fee configuration for a symbol and user tier. Uses cache for performance. */
  public TieredFeeConfig getTieredFeeConfig(String symbol, UserTier tier) {
    String cacheKey = symbol + ":" + tier.name();
    return tieredFeeConfigCache.computeIfAbsent(
        cacheKey,
        key -> {
          return tieredFeeConfigRepository
              .findBySymbolAndTierAndActive(symbol, tier, true)
              .orElseGet(() -> TieredFeeConfig.createDefault(symbol, tier));
        });
  }

  /**
   * Get fee configuration for a symbol and account. Looks up user tier and returns appropriate
   * tiered config.
   */
  public TieredFeeConfig getFeeConfigForAccount(String symbol, String accountId) {
    Account account =
        accountRepository
            .findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
    UserTier tier = account.getCurrentTierOrDefault();
    return getTieredFeeConfig(symbol, tier);
  }

  /**
   * Get fee configuration for a symbol (legacy method). Uses cache for performance.
   *
   * @deprecated Use getTieredFeeConfig or getFeeConfigForAccount instead
   */
  @Deprecated
  public FeeConfig getFeeConfig(String symbol) {
        return feeConfigCache.computeIfAbsent(symbol, s -> {
            return feeConfigRepository.findBySymbolAndActive(s, true)
                    .orElseGet(() -> getDefaultFeeConfig(s));
        });
    }

  /**
   * Calculate and deduct maker fee based on user tier.
   *
   * @param accountId Account to deduct from
   * @param symbol Trading symbol
   * @param tradeValue Total trade value
   * @param quoteAsset Quote asset (e.g., USDC)
   * @return Fee amount deducted
   */
  @Transactional
  public BigDecimal deductMakerFee(
      String accountId, String symbol, BigDecimal tradeValue, String quoteAsset) {
    TieredFeeConfig config = getFeeConfigForAccount(symbol, accountId);
        BigDecimal fee = config.calculateMakerFee(tradeValue);

    // Only deduct if fee is non-zero (VIP5 has 0% maker fee)
    if (fee.compareTo(BigDecimal.ZERO) > 0) {
      accountService.deductLockedBalance(accountId, quoteAsset, fee);
    }

    log.info(
        "Maker fee deducted: account={}, tier={}, symbol={}, fee={} {}",
        accountId,
        config.getTier(),
        symbol,
        fee,
        quoteAsset);

        return fee;
    }

  /**
   * Calculate and deduct taker fee based on user tier.
   *
   * @param accountId Account to deduct from
   * @param symbol Trading symbol
   * @param tradeValue Total trade value
   * @param quoteAsset Quote asset (e.g., USDC)
   * @return Fee amount deducted
   */
  @Transactional
  public BigDecimal deductTakerFee(
      String accountId, String symbol, BigDecimal tradeValue, String quoteAsset) {
    TieredFeeConfig config = getFeeConfigForAccount(symbol, accountId);
        BigDecimal fee = config.calculateTakerFee(tradeValue);

        accountService.deductLockedBalance(accountId, quoteAsset, fee);

    log.info(
        "Taker fee deducted: account={}, tier={}, symbol={}, fee={} {}",
        accountId,
        config.getTier(),
        symbol,
        fee,
        quoteAsset);

        return fee;
    }

  /** Calculate maker fee for an account (tier-aware). */
  public BigDecimal calculateMakerFee(String accountId, String symbol, BigDecimal tradeValue) {
    TieredFeeConfig config = getFeeConfigForAccount(symbol, accountId);
    return config.calculateMakerFee(tradeValue);
  }

  /** Calculate taker fee for an account (tier-aware). */
  public BigDecimal calculateTakerFee(String accountId, String symbol, BigDecimal tradeValue) {
    TieredFeeConfig config = getFeeConfigForAccount(symbol, accountId);
    return config.calculateTakerFee(tradeValue);
  }

  /**
   * Calculate fee without deduction (legacy, uses VIP0 rates).
   *
   * @deprecated Use calculateMakerFee(accountId, symbol, tradeValue) instead
   */
  @Deprecated
  public BigDecimal calculateMakerFee(String symbol, BigDecimal tradeValue) {
    TieredFeeConfig config = getTieredFeeConfig(symbol, UserTier.VIP0);
        return config.calculateMakerFee(tradeValue);
    }

  /**
   * Calculate fee without deduction (legacy, uses VIP0 rates).
   *
   * @deprecated Use calculateTakerFee(accountId, symbol, tradeValue) instead
   */
  @Deprecated
  public BigDecimal calculateTakerFee(String symbol, BigDecimal tradeValue) {
    TieredFeeConfig config = getTieredFeeConfig(symbol, UserTier.VIP0);
        return config.calculateTakerFee(tradeValue);
    }

  /** Zero-allocation fee calculation using scaled arithmetic (tier-aware). */
  public long calculateFeeScaled(
      String accountId, String symbol, long tradeValueScaled, boolean isMaker) {
    TieredFeeConfig config = getFeeConfigForAccount(symbol, accountId);
    return config.calculateFeeScaled(tradeValueScaled, isMaker);
  }

  /**
   * Zero-allocation fee calculation using scaled arithmetic (legacy).
   *
   * @deprecated Use calculateFeeScaled(accountId, symbol, tradeValueScaled, isMaker) instead
   */
  @Deprecated
  public long calculateFeeScaled(String symbol, long tradeValueScaled, boolean isMaker) {
    TieredFeeConfig config = getTieredFeeConfig(symbol, UserTier.VIP0);
        return config.calculateFeeScaled(tradeValueScaled, isMaker);
    }

  /** Reload fee config caches from database. */
  public void reloadFeeConfigs() {
    // Reload legacy configs
    feeConfigCache.clear();
        feeConfigRepository.findAll().forEach(config -> {
            if (config.isActive()) {
                feeConfigCache.put(config.getSymbol(), config);
            }
        });

    // Reload tiered configs
    tieredFeeConfigCache.clear();
    tieredFeeConfigRepository
        .findByActive(true)
        .forEach(
            config -> {
              String cacheKey = config.getSymbol() + ":" + config.getTier().name();
              tieredFeeConfigCache.put(cacheKey, config);
            });

    log.info(
        "Fee configs reloaded: {} legacy symbols, {} tiered configs",
        feeConfigCache.size(),
        tieredFeeConfigCache.size());
    }

    /**
     * Default fee configuration if not found in database.
     */
    private FeeConfig getDefaultFeeConfig(String symbol) {
        return FeeConfig.builder()
                .symbol(symbol)
                .makerFeeBps(10) // 0.1% maker fee
                .takerFeeBps(20) // 0.2% taker fee
                .minFee(BigDecimal.valueOf(0.01))
                .tier(0)
                .active(true)
                .build();
    }
}

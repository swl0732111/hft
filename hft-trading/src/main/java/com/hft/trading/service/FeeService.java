package com.hft.trading.service;

import com.hft.trading.domain.FeeConfig;
import com.hft.trading.repository.FeeConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fee calculation and deduction service.
 * Supports Maker/Taker fee model with configurable rates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeService {
    private final FeeConfigRepository feeConfigRepository;
    private final AccountService accountService;

    // Cache for fee configs (reload periodically)
    private final Map<String, FeeConfig> feeConfigCache = new ConcurrentHashMap<>();

    /**
     * Get fee configuration for a symbol.
     * Uses cache for performance.
     */
    public FeeConfig getFeeConfig(String symbol) {
        return feeConfigCache.computeIfAbsent(symbol, s -> {
            return feeConfigRepository.findBySymbolAndActive(s, true)
                    .orElseGet(() -> getDefaultFeeConfig(s));
        });
    }

    /**
     * Calculate and deduct maker fee.
     * 
     * @param accountId  Account to deduct from
     * @param symbol     Trading symbol
     * @param tradeValue Total trade value
     * @param quoteAsset Quote asset (e.g., USDC)
     * @return Fee amount deducted
     */
    @Transactional
    public BigDecimal deductMakerFee(String accountId, String symbol,
            BigDecimal tradeValue, String quoteAsset) {
        FeeConfig config = getFeeConfig(symbol);
        BigDecimal fee = config.calculateMakerFee(tradeValue);

        accountService.deductLockedBalance(accountId, quoteAsset, fee);

        log.info("Maker fee deducted: account={}, symbol={}, fee={} {}",
                accountId, symbol, fee, quoteAsset);

        return fee;
    }

    /**
     * Calculate and deduct taker fee.
     * 
     * @param accountId  Account to deduct from
     * @param symbol     Trading symbol
     * @param tradeValue Total trade value
     * @param quoteAsset Quote asset (e.g., USDC)
     * @return Fee amount deducted
     */
    @Transactional
    public BigDecimal deductTakerFee(String accountId, String symbol,
            BigDecimal tradeValue, String quoteAsset) {
        FeeConfig config = getFeeConfig(symbol);
        BigDecimal fee = config.calculateTakerFee(tradeValue);

        accountService.deductLockedBalance(accountId, quoteAsset, fee);

        log.info("Taker fee deducted: account={}, symbol={}, fee={} {}",
                accountId, symbol, fee, quoteAsset);

        return fee;
    }

    /**
     * Calculate fee without deduction (for display purposes).
     */
    public BigDecimal calculateMakerFee(String symbol, BigDecimal tradeValue) {
        FeeConfig config = getFeeConfig(symbol);
        return config.calculateMakerFee(tradeValue);
    }

    /**
     * Calculate fee without deduction (for display purposes).
     */
    public BigDecimal calculateTakerFee(String symbol, BigDecimal tradeValue) {
        FeeConfig config = getFeeConfig(symbol);
        return config.calculateTakerFee(tradeValue);
    }

    /**
     * Zero-allocation fee calculation using scaled arithmetic.
     */
    public long calculateFeeScaled(String symbol, long tradeValueScaled, boolean isMaker) {
        FeeConfig config = getFeeConfig(symbol);
        return config.calculateFeeScaled(tradeValueScaled, isMaker);
    }

    /**
     * Reload fee config cache from database.
     */
    public void reloadFeeConfigs() {
        feeConfigCache.clear();
        feeConfigRepository.findAll().forEach(config -> {
            if (config.isActive()) {
                feeConfigCache.put(config.getSymbol(), config);
            }
        });
        log.info("Fee configs reloaded: {} symbols", feeConfigCache.size());
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

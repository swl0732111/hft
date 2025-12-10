package com.hft.trading.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hft.common.domain.AccountBalance;
import com.hft.trading.service.RocksDBService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Enhanced state store for complete account state in RocksDB.
 * Supports balances, positions, and risk metrics.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnhancedAccountStateStore {

    private final RocksDBService rocksDBService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get complete account state
     */
    public AccountState getAccountState(String accountId) {
        String key = getAccountStateKey(accountId);
        byte[] value = rocksDBService.get(key);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, AccountState.class);
        } catch (IOException e) {
            log.error("Failed to deserialize account state for {}", accountId, e);
            return null;
        }
    }

    /**
     * Save complete account state
     */
    public void saveAccountState(AccountState state) {
        String key = getAccountStateKey(state.getAccountId());
        try {
            byte[] value = objectMapper.writeValueAsBytes(state);
            rocksDBService.put(key, value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize account state for {}", state.getAccountId(), e);
        }
    }

    /**
     * Get or create account state
     */
    public AccountState getOrCreateAccountState(String accountId) {
        AccountState state = getAccountState(accountId);
        if (state == null) {
            state = AccountState.builder()
                    .accountId(accountId)
                    .lastUpdateTime(System.currentTimeMillis())
                    .build();
        }
        return state;
    }

    /**
     * Update asset balance in account state
     */
    public void updateBalance(String accountId, AccountBalance balance) {
        AccountState state = getOrCreateAccountState(accountId);
        state.updateBalance(
                balance.getAsset(),
                balance.getAvailableBalance(),
                balance.getLockedBalance());
        state.updateRiskMetrics();
        saveAccountState(state);
    }

    /**
     * Update position
     */
    public void updatePosition(String accountId, String symbol, AccountState.Position position) {
        AccountState state = getOrCreateAccountState(accountId);
        state.updatePosition(symbol, position);
        saveAccountState(state);
    }

    /**
     * Close position
     */
    public void closePosition(String accountId, String symbol) {
        AccountState state = getAccountState(accountId);
        if (state != null) {
            state.closePosition(symbol);
            saveAccountState(state);
        }
    }

    /**
     * Update risk metrics
     */
    public void updateRiskMetrics(String accountId, AccountState.RiskMetrics metrics) {
        AccountState state = getOrCreateAccountState(accountId);
        state.setRiskMetrics(metrics);
        state.updateRiskMetrics();
        saveAccountState(state);
    }

    /**
     * Get single asset balance (backward compatibility)
     */
    public AccountBalance getBalance(String accountId, String asset) {
        AccountState state = getAccountState(accountId);
        if (state == null || state.getBalances() == null) {
            return null;
        }

        AccountState.AssetBalance assetBalance = state.getBalances().get(asset);
        if (assetBalance == null) {
            return null;
        }

        return AccountBalance.builder()
                .accountId(accountId)
                .asset(asset)
                .availableBalance(assetBalance.getAvailableBalance())
                .lockedBalance(assetBalance.getLockedBalance())
                .build();
    }

    private String getAccountStateKey(String accountId) {
        return "account:state:" + accountId;
    }
}

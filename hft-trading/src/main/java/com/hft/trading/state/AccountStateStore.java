package com.hft.trading.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hft.common.domain.AccountBalance;
import com.hft.trading.service.RocksDBService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountStateStore {

    private final RocksDBService rocksDBService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void updateBalance(AccountBalance balance) {
        String key = getBalanceKey(balance.getAccountId(), balance.getAsset());
        try {
            byte[] value = objectMapper.writeValueAsBytes(balance);
            rocksDBService.put(key, value);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize balance for key {}", key, e);
        }
    }

    public AccountBalance getBalance(String accountId, String asset) {
        String key = getBalanceKey(accountId, asset);
        byte[] value = rocksDBService.get(key);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, AccountBalance.class);
        } catch (IOException e) {
            log.error("Failed to deserialize balance for key {}", key, e);
            return null;
        }
    }

    private String getBalanceKey(String accountId, String asset) {
        return "balance:" + accountId + ":" + asset;
    }
}

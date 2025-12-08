package com.hft.wallet.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${trading.service.url:http://localhost:8080}")
    private String tradingServiceUrl;

    public void creditBalance(String accountId, String asset, BigDecimal amount) {
        String url = tradingServiceUrl + "/internal/accounts/credit";
        CreditRequest request = new CreditRequest(accountId, asset, amount);

        try {
            restTemplate.postForEntity(url, request, Void.class);
            log.info("Successfully credited balance for account {}", accountId);
        } catch (Exception e) {
            log.error("Failed to credit balance for account {}", accountId, e);
            throw new RuntimeException("Failed to credit balance", e);
        }
    }

    public BigDecimal getTotalBalance(String asset) {
        String url = tradingServiceUrl + "/internal/accounts/total-balance/" + asset;

        try {
            ResponseEntity<BigDecimal> response = restTemplate.getForEntity(url, BigDecimal.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get total balance for asset {}", asset, e);
            throw new RuntimeException("Failed to get total balance", e);
        }
    }

    @Data
    @AllArgsConstructor
    private static class CreditRequest {
        private String accountId;
        private String asset;
        private BigDecimal amount;
    }
}

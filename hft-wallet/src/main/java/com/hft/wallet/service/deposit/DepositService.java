package com.hft.wallet.service.deposit;

import com.hft.wallet.client.TradingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service to coordinate deposit processing and notify trading system.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepositService {

    private final TradingClient tradingClient;

    public void processDeposit(String accountId, BigDecimal amount, String asset, String txHash, String type) {
        log.info("Processing {} deposit for account {}: {} {} (Tx: {})", type, accountId, amount, asset, txHash);

        tradingClient.creditBalance(accountId, asset, amount);

        log.info("Successfully processed deposit for account {}", accountId);
    }
}

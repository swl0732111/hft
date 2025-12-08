package com.hft.wallet.service;

import com.hft.wallet.client.TradingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final TradingClient tradingClient;
    // In a real app, we would inject services to query actual on-chain balances
    // private final OnChainBalanceService onChainBalanceService;

    public boolean verifyReserves(String asset) {
        log.info("Starting reconciliation for {}", asset);

        // 1. Get total internal liabilities (User balances)
        BigDecimal internalLiabilities = tradingClient.getTotalBalance(asset);
        if (internalLiabilities == null)
            internalLiabilities = BigDecimal.ZERO;

        // 2. Get total on-chain assets (Custodial + Hot Wallet)
        // Mocked for now
        BigDecimal onChainAssets = getMockOnChainAssets(asset);

        log.info("Reconciliation Report for {}: Internal Liabilities = {}, On-Chain Assets = {}",
                asset, internalLiabilities, onChainAssets);

        if (internalLiabilities.compareTo(onChainAssets) > 0) {
            log.error("CRITICAL: Reserves insufficient! Deficit: {}", internalLiabilities.subtract(onChainAssets));
            return false;
        }

        log.info("Reserves verified. Surplus: {}", onChainAssets.subtract(internalLiabilities));
        return true;
    }

    private BigDecimal getMockOnChainAssets(String asset) {
        // Mock logic: Always return slightly more than liabilities to simulate solvent
        // state
        // In reality, this would query the blockchain node
        return new BigDecimal("1000000");
    }
}

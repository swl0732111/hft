package com.hft.wallet.service.deposit;

import java.math.BigDecimal;

/**
 * Interface for listening to blockchain deposit events.
 */
public interface DepositListener {

    /**
     * Check for new deposits.
     * In a real implementation, this might process a block or poll an API.
     */
    void checkDeposits();

    /**
     * Process a specific transaction (simulated for now).
     */
    void processTransaction(String txHash, String from, String to, BigDecimal amount, String asset, String chain);
}

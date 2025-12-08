package com.hft.wallet.service.deposit;

import com.hft.wallet.domain.WalletConnection;
import com.hft.wallet.repository.WalletConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Listener for deposits to the platform hot wallet from non-custodial wallets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NonCustodialDepositListener implements DepositListener {

    private final WalletConnectionRepository walletConnectionRepository;
    private final DepositService depositService;

    @Value("${wallet.hot-wallet.address:0x71C7656EC7ab88b098defB751B7401B5f6d8976F}")
    private String platformHotWalletAddress;

    @Override
    public void checkDeposits() {
        // In a real implementation, this would scan blocks for transactions
        // TO the platformHotWalletAddress.
        log.debug("Checking non-custodial deposits...");
    }

    @Override
    public void processTransaction(String txHash, String from, String to, BigDecimal amount, String asset,
            String chain) {
        // Check if transaction is sent TO the platform hot wallet
        if (to.equalsIgnoreCase(platformHotWalletAddress)) {

            // Identify user by FROM address
            // Note: In a real multi-chain system, we'd also check the chain ID
            Optional<WalletConnection> connectionOpt = walletConnectionRepository.findByWalletAddressAndChain(from,
                    chain);

            if (connectionOpt.isPresent()) {
                WalletConnection connection = connectionOpt.get();
                log.info("Detected non-custodial deposit: {} {} from {} (User: {})", amount, asset, from,
                        connection.getAccountId());

                depositService.processDeposit(connection.getAccountId(), amount, asset, txHash, "NON_CUSTODIAL");
            } else {
                log.warn("Received deposit from unknown address: {}", from);
                // Handle unknown deposit (e.g., flag for manual review)
            }
        }
    }
}

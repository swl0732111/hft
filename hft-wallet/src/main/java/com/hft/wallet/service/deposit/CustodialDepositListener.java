package com.hft.wallet.service.deposit;

import com.hft.wallet.domain.CustodialWallet;
import com.hft.wallet.repository.CustodialWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Listener for deposits to custodial wallets (user-specific addresses).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustodialDepositListener implements DepositListener {

    private final CustodialWalletRepository custodialWalletRepository;
    private final DepositService depositService;

    @Override
    public void checkDeposits() {
        // In a real implementation, this would scan blocks for transactions
        // to any address in custodialWalletRepository.
        log.debug("Checking custodial deposits...");
    }

    @Override
    public void processTransaction(String txHash, String from, String to, BigDecimal amount, String asset,
            String chain) {
        // Check if 'to' address belongs to a custodial wallet
        Optional<CustodialWallet> walletOpt = custodialWalletRepository.findByWalletAddress(to);

        if (walletOpt.isPresent()) {
            CustodialWallet wallet = walletOpt.get();
            log.info("Detected custodial deposit: {} {} to {} (User: {})", amount, asset, to, wallet.getAccountId());

            depositService.processDeposit(wallet.getAccountId(), amount, asset, txHash, "CUSTODIAL");
        }
    }
}

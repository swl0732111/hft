package com.hft.wallet.service;

import com.hft.wallet.domain.CustodialWallet;
import com.hft.wallet.repository.CustodialWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing custodial wallets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustodialWalletService {

    private final CustodialWalletRepository custodialWalletRepository;
    private final EncryptionService encryptionService;

    /**
     * Get or create a deposit address for the account.
     */
    @Transactional
    public String getDepositAddress(String accountId, String chain) {
        Optional<CustodialWallet> existing = custodialWalletRepository.findByAccountIdAndChain(accountId, chain);
        if (existing.isPresent()) {
            return existing.get().getWalletAddress();
        }

        return createWallet(accountId, chain).getWalletAddress();
    }

    /**
     * Create a new custodial wallet.
     */
    private CustodialWallet createWallet(String accountId, String chain) {
        try {
            // Generate new key pair
            ECKeyPair keyPair = Keys.createEcKeyPair();
            BigInteger privateKey = keyPair.getPrivateKey();
            BigInteger publicKey = keyPair.getPublicKey();

            String address = "0x" + Keys.getAddress(publicKey);
            String privateKeyHex = privateKey.toString(16);

            // Encrypt private key
            String encryptedKey = encryptionService.encrypt(privateKeyHex);

            CustodialWallet wallet = CustodialWallet.builder()
                    .id(UUID.randomUUID().toString())
                    .accountId(accountId)
                    .walletAddress(address)
                    .encryptedPrivateKey(encryptedKey)
                    .chain(chain)
                    .createdAt(System.currentTimeMillis())
                    .build();

            log.info("Created new custodial wallet for account: {} on chain: {}", accountId, chain);
            return custodialWalletRepository.save(wallet);

        } catch (Exception e) {
            log.error("Error creating custodial wallet", e);
            throw new RuntimeException("Failed to create wallet", e);
        }
    }

    /**
     * Get decrypted private key (Internal use only).
     */
    public String getPrivateKey(String walletAddress) {
        CustodialWallet wallet = custodialWalletRepository.findByWalletAddress(walletAddress)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        return encryptionService.decrypt(wallet.getEncryptedPrivateKey());
    }
}

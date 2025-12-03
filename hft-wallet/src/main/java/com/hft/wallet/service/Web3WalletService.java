package com.hft.wallet.service;

import com.hft.wallet.domain.WalletConnection;
import com.hft.wallet.domain.WalletNonce;
import com.hft.wallet.repository.WalletConnectionRepository;
import com.hft.wallet.repository.WalletNonceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Web3 wallet operations.
 * Handles signature verification, nonce management, and wallet connections.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Web3WalletService {

    private final WalletConnectionRepository walletConnectionRepository;
    private final WalletNonceRepository walletNonceRepository;

    private static final long NONCE_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes
    private static final String MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n";

    /**
     * Generate a new nonce for wallet signature
     */
    @Transactional
    public String generateNonce(String walletAddress) {
        String checksumAddress = toChecksumAddress(walletAddress);
        long now = System.currentTimeMillis();

        // Invalidate any existing nonces
        walletNonceRepository.findByWalletAddressAndUsedFalseAndExpiresAtGreaterThan(
                checksumAddress, now).ifPresent(existingNonce -> {
                    existingNonce.setUsed(true);
                    existingNonce.setUsedAt(now);
                    walletNonceRepository.save(existingNonce);
                });

        // Generate new nonce
        String nonce = UUID.randomUUID().toString();
        WalletNonce walletNonce = WalletNonce.builder()
                .id(UUID.randomUUID().toString())
                .walletAddress(checksumAddress)
                .nonce(nonce)
                .createdAt(now)
                .expiresAt(now + NONCE_EXPIRY_MS)
                .used(false)
                .build();

        walletNonceRepository.save(walletNonce);
        log.info("Generated nonce for wallet: {}", checksumAddress);

        return nonce;
    }

    /**
     * Verify wallet signature
     */
    @Transactional
    public boolean verifySignature(String walletAddress, String signature, String nonce) {
        try {
            String checksumAddress = toChecksumAddress(walletAddress);
            long now = System.currentTimeMillis();

            // Verify nonce exists and is valid
            Optional<WalletNonce> nonceOpt = walletNonceRepository
                    .findByWalletAddressAndUsedFalseAndExpiresAtGreaterThan(checksumAddress, now);

            if (nonceOpt.isEmpty()) {
                log.warn("No valid nonce found for wallet: {}", checksumAddress);
                return false;
            }

            WalletNonce walletNonce = nonceOpt.get();
            if (!walletNonce.getNonce().equals(nonce)) {
                log.warn("Nonce mismatch for wallet: {}", checksumAddress);
                return false;
            }

            // Build message
            String message = buildSignatureMessage(checksumAddress, nonce, walletNonce.getCreatedAt());

            // Verify signature
            String recoveredAddress = recoverAddressFromSignature(message, signature);
            boolean isValid = checksumAddress.equalsIgnoreCase(recoveredAddress);

            if (isValid) {
                // Mark nonce as used
                walletNonce.setUsed(true);
                walletNonce.setUsedAt(now);
                walletNonceRepository.save(walletNonce);
                log.info("Signature verified for wallet: {}", checksumAddress);
            } else {
                log.warn("Signature verification failed for wallet: {}", checksumAddress);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error verifying signature", e);
            return false;
        }
    }

    /**
     * Link wallet to account
     */
    @Transactional
    public WalletConnection linkWallet(String accountId, String walletAddress, String chain, Long chainId) {
        String checksumAddress = toChecksumAddress(walletAddress);
        long now = System.currentTimeMillis();

        // Check if wallet already exists
        Optional<WalletConnection> existing = walletConnectionRepository
                .findByWalletAddressAndChain(checksumAddress, chain);

        if (existing.isPresent()) {
            WalletConnection connection = existing.get();
            connection.setAccountId(accountId);
            connection.setStatus(WalletConnection.ConnectionStatus.ACTIVE);
            connection.setLastActiveAt(now);
            return walletConnectionRepository.save(connection);
        }

        // Check if this is the first wallet for the account
        List<WalletConnection> accountWallets = walletConnectionRepository.findByAccountId(accountId);
        boolean isPrimary = accountWallets.isEmpty();

        // Create new wallet connection
        WalletConnection connection = WalletConnection.builder()
                .id(UUID.randomUUID().toString())
                .accountId(accountId)
                .walletAddress(checksumAddress)
                .chain(chain)
                .chainId(chainId)
                .status(WalletConnection.ConnectionStatus.ACTIVE)
                .connectedAt(now)
                .lastActiveAt(now)
                .isPrimary(isPrimary)
                .build();

        log.info("Linked wallet {} to account {}", checksumAddress, accountId);
        return walletConnectionRepository.save(connection);
    }

    /**
     * Get wallet connections for account
     */
    public List<WalletConnection> getAccountWallets(String accountId) {
        return walletConnectionRepository.findByAccountId(accountId);
    }

    /**
     * Disconnect wallet
     */
    @Transactional
    public void disconnectWallet(String walletAddress, String chain) {
        String checksumAddress = toChecksumAddress(walletAddress);
        walletConnectionRepository.findByWalletAddressAndChain(checksumAddress, chain)
                .ifPresent(connection -> {
                    connection.setStatus(WalletConnection.ConnectionStatus.DISCONNECTED);
                    walletConnectionRepository.save(connection);
                    log.info("Disconnected wallet: {}", checksumAddress);
                });
    }

    /**
     * Build signature message
     */
    private String buildSignatureMessage(String walletAddress, String nonce, Long timestamp) {
        return String.format(
                "Welcome to HFT Trading Platform!\n\n" +
                        "Sign this message to verify your wallet ownership.\n\n" +
                        "Wallet: %s\n" +
                        "Nonce: %s\n" +
                        "Timestamp: %d\n\n" +
                        "This request will not trigger a blockchain transaction or cost any gas fees.",
                walletAddress, nonce, timestamp);
    }

    /**
     * Recover address from signature (EIP-191)
     */
    private String recoverAddressFromSignature(String message, String signature) throws SignatureException {
        byte[] messageBytes = message.getBytes();
        byte[] messageHash = getEthereumMessageHash(messageBytes);

        // Parse signature
        byte[] signatureBytes = Numeric.hexStringToByteArray(signature);
        byte v = signatureBytes[64];
        if (v < 27) {
            v += 27;
        }

        Sign.SignatureData signatureData = new Sign.SignatureData(
                v,
                Arrays.copyOfRange(signatureBytes, 0, 32),
                Arrays.copyOfRange(signatureBytes, 32, 64));

        // Recover public key
        BigInteger publicKey = Sign.signedMessageHashToKey(messageHash, signatureData);

        // Derive address from public key
        return "0x" + Keys.getAddress(publicKey);
    }

    /**
     * Get Ethereum message hash (EIP-191)
     */
    private byte[] getEthereumMessageHash(byte[] message) {
        String prefix = MESSAGE_PREFIX + message.length;
        byte[] prefixBytes = prefix.getBytes();

        byte[] result = new byte[prefixBytes.length + message.length];
        System.arraycopy(prefixBytes, 0, result, 0, prefixBytes.length);
        System.arraycopy(message, 0, result, prefixBytes.length, message.length);

        return org.web3j.crypto.Hash.sha3(result);
    }

    /**
     * Convert address to checksum format (EIP-55)
     */
    private String toChecksumAddress(String address) {
        try {
            return Keys.toChecksumAddress(address.toLowerCase());
        } catch (Exception e) {
            log.error("Error converting address to checksum format", e);
            return address.toLowerCase();
        }
    }

    /**
     * Clean up expired nonces (scheduled task)
     */
    @Transactional
    public void cleanupExpiredNonces() {
        long now = System.currentTimeMillis();
        walletNonceRepository.deleteByExpiresAtLessThan(now);
        log.debug("Cleaned up expired nonces");
    }
}

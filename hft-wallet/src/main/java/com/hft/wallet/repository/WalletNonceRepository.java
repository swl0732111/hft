package com.hft.wallet.repository;

import com.hft.wallet.domain.WalletNonce;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for WalletNonce entities.
 */
@Repository
public interface WalletNonceRepository extends CrudRepository<WalletNonce, String> {

    /**
     * Find active (unused, non-expired) nonce for wallet address
     */
    Optional<WalletNonce> findByWalletAddressAndUsedFalseAndExpiresAtGreaterThan(
            String walletAddress, Long currentTime);

    /**
     * Find nonce by value
     */
    Optional<WalletNonce> findByNonce(String nonce);

    /**
     * Delete expired nonces
     */
    void deleteByExpiresAtLessThan(Long currentTime);
}

package com.hft.wallet.repository;

import com.hft.wallet.domain.WalletConnection;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WalletConnection entities.
 */
@Repository
public interface WalletConnectionRepository extends CrudRepository<WalletConnection, String> {

    /**
     * Find wallet connection by wallet address and chain
     */
    Optional<WalletConnection> findByWalletAddress(String walletAddress);

    Optional<WalletConnection> findByWalletAddressAndChain(String walletAddress, String chain);

    /**
     * Find all wallet connections for an account
     */
    List<WalletConnection> findByAccountId(String accountId);

    /**
     * Find primary wallet for an account
     */
    Optional<WalletConnection> findByAccountIdAndIsPrimaryTrue(String accountId);

    /**
     * Check if wallet address exists
     */
    boolean existsByWalletAddress(String walletAddress);
}

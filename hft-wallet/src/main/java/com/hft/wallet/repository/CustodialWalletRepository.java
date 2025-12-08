package com.hft.wallet.repository;

import com.hft.wallet.domain.CustodialWallet;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for CustodialWallet entities.
 */
@Repository
public interface CustodialWalletRepository extends CrudRepository<CustodialWallet, String> {

    /**
     * Find custodial wallet by account ID and chain
     */
    Optional<CustodialWallet> findByAccountIdAndChain(String accountId, String chain);

    /**
     * Find custodial wallet by address
     */
    Optional<CustodialWallet> findByWalletAddress(String walletAddress);
}

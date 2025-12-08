package com.hft.wallet.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.annotation.Transient;

/**
 * Represents a custodial wallet managed by the platform.
 * Stores encrypted private keys for automated operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("CUSTODIAL_WALLETS")
public class CustodialWallet implements Persistable<String> {

    @Id
    private String id;

    /**
     * Associated account ID
     */
    private String accountId;

    /**
     * Wallet address (checksummed)
     */
    private String walletAddress;

    /**
     * Encrypted private key
     */
    private String encryptedPrivateKey;

    /**
     * Blockchain chain (e.g., "ETH")
     */
    private String chain;

    /**
     * Timestamp when wallet was created
     */
    private Long createdAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }
}

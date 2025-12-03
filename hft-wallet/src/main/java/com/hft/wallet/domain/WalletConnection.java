package com.hft.wallet.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Represents a Web3 wallet connection.
 * Stores wallet address, chain information, and connection status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("wallet_connections")
public class WalletConnection {

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
     * Blockchain chain (e.g., "ethereum", "polygon", "bsc")
     */
    private String chain;

    /**
     * Chain ID (e.g., 1 for Ethereum mainnet, 137 for Polygon)
     */
    private Long chainId;

    /**
     * Connection status
     */
    private ConnectionStatus status;

    /**
     * Timestamp when wallet was first connected
     */
    private Long connectedAt;

    /**
     * Timestamp of last activity
     */
    private Long lastActiveAt;

    /**
     * Whether this is the primary wallet for the account
     */
    private Boolean isPrimary;

    /**
     * Optional wallet label/name
     */
    private String label;

    public enum ConnectionStatus {
        ACTIVE,
        DISCONNECTED,
        SUSPENDED
    }
}

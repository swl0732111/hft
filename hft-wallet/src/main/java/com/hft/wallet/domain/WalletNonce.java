package com.hft.wallet.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Stores nonces for wallet signature verification. Nonces are single-use tokens to prevent replay
 * attacks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("WALLET_NONCES")
public class WalletNonce implements org.springframework.data.domain.Persistable<String> {

    @Id
    private String id;

    /**
     * Wallet address (checksummed)
     */
    private String walletAddress;

    /**
     * Unique nonce value
     */
    private String nonce;

    /**
     * Timestamp when nonce was created
     */
    private Long createdAt;

    /**
     * Timestamp when nonce expires
     */
    private Long expiresAt;

    /**
     * Whether nonce has been used
     */
    private Boolean used;

    /**
     * Timestamp when nonce was used
     */
    private Long usedAt;

  @org.springframework.data.annotation.Transient @Builder.Default private boolean isNew = true;

  @Override
  public boolean isNew() {
    return isNew;
  }
}

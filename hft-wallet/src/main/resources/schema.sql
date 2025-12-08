-- Web3 Wallet tables
CREATE TABLE IF NOT EXISTS wallet_connections (
    id VARCHAR(255) PRIMARY KEY,
    account_id VARCHAR(255),
    wallet_address VARCHAR(255) NOT NULL,
    chain VARCHAR(50) NOT NULL,
    chain_id BIGINT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    connected_at BIGINT,
    last_active_at BIGINT,
    is_primary BOOLEAN DEFAULT false,
    label VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS wallet_nonces (
    id VARCHAR(255) PRIMARY KEY,
    wallet_address VARCHAR(255) NOT NULL,
    nonce VARCHAR(255) NOT NULL UNIQUE,
    created_at BIGINT,
    expires_at BIGINT,
    used BOOLEAN DEFAULT false,
    used_at BIGINT
);

-- Indexes for wallet tables
CREATE INDEX IF NOT EXISTS idx_wallet_connections_account ON wallet_connections(account_id);
CREATE INDEX IF NOT EXISTS idx_wallet_connections_address ON wallet_connections(wallet_address);
CREATE INDEX IF NOT EXISTS idx_wallet_nonces_address ON wallet_nonces(wallet_address);
CREATE INDEX IF NOT EXISTS idx_wallet_nonces_expires ON wallet_nonces(expires_at);

CREATE TABLE IF NOT EXISTS custodial_wallets (
    id VARCHAR(255) PRIMARY KEY,
    account_id VARCHAR(255),
    wallet_address VARCHAR(255) NOT NULL UNIQUE,
    encrypted_private_key VARCHAR(1000) NOT NULL,
    chain VARCHAR(50) NOT NULL,
    created_at BIGINT
);

CREATE INDEX IF NOT EXISTS idx_custodial_wallets_account ON custodial_wallets(account_id);
CREATE INDEX IF NOT EXISTS idx_custodial_wallets_address ON custodial_wallets(wallet_address);
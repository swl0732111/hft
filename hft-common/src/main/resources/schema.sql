CREATE TABLE IF NOT EXISTS wallet_nonces (
    id VARCHAR(255) PRIMARY KEY,
    wallet_address VARCHAR(255) NOT NULL,
    nonce VARCHAR(255) NOT NULL,
    created_at BIGINT,
    expires_at BIGINT,
    used BOOLEAN,
    used_at BIGINT
);

CREATE TABLE IF NOT EXISTS wallet_connections (
    id VARCHAR(255) PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    wallet_address VARCHAR(255) NOT NULL,
    chain VARCHAR(50),
    chain_id BIGINT,
    status VARCHAR(50),
    connected_at BIGINT,
    last_active_at BIGINT,
    is_primary BOOLEAN,
    label VARCHAR(255)
);

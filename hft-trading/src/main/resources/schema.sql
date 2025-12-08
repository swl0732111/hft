CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(255) PRIMARY KEY,
    account_id VARCHAR(255),
    wallet_address VARCHAR(255),
    symbol VARCHAR(50),
    side VARCHAR(10),
    chain VARCHAR(20),
    price DECIMAL(20, 8),
    quantity DECIMAL(20, 8),
    initial_quantity DECIMAL(20, 8),
    price_scaled BIGINT,
    quantity_scaled BIGINT,
    initial_quantity_scaled BIGINT,
    timestamp BIGINT,
    status VARCHAR(20),
    order_type VARCHAR(20) DEFAULT 'LIMIT',
    display_quantity_scaled BIGINT,
    hidden_quantity_scaled BIGINT,
    stop_price_scaled BIGINT,
    triggered BOOLEAN DEFAULT false
);

CREATE TABLE IF NOT EXISTS transaction_log (
    id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(50),
    order_id VARCHAR(255),
    symbol VARCHAR(50),
    timestamp BIGINT,
    details TEXT
);

CREATE TABLE IF NOT EXISTS accounts (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) UNIQUE,
    username VARCHAR(100),
    email VARCHAR(255),
    created_at BIGINT,
    status VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS account_balances (
    id VARCHAR(255) PRIMARY KEY,
    account_id VARCHAR(255),
    asset VARCHAR(50),
    type VARCHAR(20) DEFAULT 'SPOT',
    available_balance DECIMAL(20, 8),
    locked_balance DECIMAL(20, 8) DEFAULT 0,
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_account_balances_unique ON account_balances(account_id, asset, type);

CREATE TABLE IF NOT EXISTS fee_config (
    id VARCHAR(255) PRIMARY KEY,
    symbol VARCHAR(50) NOT NULL,
    maker_fee_bps INT NOT NULL,
    taker_fee_bps INT NOT NULL,
    min_fee DECIMAL(20, 8) DEFAULT 0.01,
    tier INT DEFAULT 0,
    active BOOLEAN DEFAULT true
);

-- Default fee configuration
INSERT INTO fee_config (id, symbol, maker_fee_bps, taker_fee_bps, min_fee, tier, active)
VALUES 
    ('default-btc-usdc', 'BTC-USDC', 10, 20, 0.01, 0, true),
    ('default-eth-usdc', 'ETH-USDC', 10, 20, 0.01, 0, true),
    ('default-sol-usdc', 'SOL-USDC', 10, 20, 0.01, 0, true);



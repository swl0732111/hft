-- Migration for Tiered Fee System
-- Adds support for user VIP tiers based on 30-day trading volume

-- 1. Add tier-related columns to accounts table
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS current_tier VARCHAR(10) DEFAULT 'VIP0';
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS volume_30d_scaled BIGINT DEFAULT 0;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS last_tier_update BIGINT DEFAULT 0;
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS tier_locked_until BIGINT;

-- 2. Create tier_config table
CREATE TABLE IF NOT EXISTS tier_config (
    id VARCHAR(36) PRIMARY KEY,
    tier VARCHAR(10) NOT NULL UNIQUE,
    min_volume_scaled BIGINT NOT NULL,
    max_volume_scaled BIGINT NOT NULL,
    maker_fee_bps INT NOT NULL,
    taker_fee_bps INT NOT NULL,
    api_rate_limit_rps INT NOT NULL,
    support_priority INT NOT NULL,
    dedicated_account_manager BOOLEAN DEFAULT FALSE,
    priority_withdrawal BOOLEAN DEFAULT FALSE,
    custom_api_solutions BOOLEAN DEFAULT FALSE,
    description TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

-- 3. Create tiered_fee_config table
CREATE TABLE IF NOT EXISTS tiered_fee_config (
    id VARCHAR(36) PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    tier VARCHAR(10) NOT NULL,
    maker_fee_bps INT NOT NULL,
    taker_fee_bps INT NOT NULL,
    min_fee DECIMAL(20, 8) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE(symbol, tier)
);

CREATE INDEX IF NOT EXISTS idx_tiered_fee_config_symbol ON tiered_fee_config(symbol);
CREATE INDEX IF NOT EXISTS idx_tiered_fee_config_tier ON tiered_fee_config(tier);

-- 4. Create trading_volume_stats table
CREATE TABLE IF NOT EXISTS trading_volume_stats (
    id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL,
    date DATE NOT NULL,
    volume_scaled BIGINT NOT NULL DEFAULT 0,
    trade_count INT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    UNIQUE(account_id, date)
);

CREATE INDEX IF NOT EXISTS idx_trading_volume_stats_account_date ON trading_volume_stats(account_id, date);
CREATE INDEX IF NOT EXISTS idx_trading_volume_stats_date ON trading_volume_stats(date);

-- 5. Insert default tier configurations
INSERT INTO tier_config (id, tier, min_volume_scaled, max_volume_scaled, maker_fee_bps, taker_fee_bps, 
                         api_rate_limit_rps, support_priority, dedicated_account_manager, 
                         priority_withdrawal, custom_api_solutions, description, active, created_at, updated_at)
VALUES 
    ('tier-vip0', 'VIP0', 0, 10000000000000, 10, 20, 10, 6, FALSE, FALSE, FALSE, 
     'Default tier for all users', TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('tier-vip1', 'VIP1', 10000000000000, 50000000000000, 8, 18, 20, 5, FALSE, FALSE, FALSE, 
     'Bronze tier - Reduced fees', TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('tier-vip2', 'VIP2', 50000000000000, 200000000000000, 6, 15, 50, 4, FALSE, TRUE, FALSE, 
     'Silver tier - Priority support', TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('tier-vip3', 'VIP3', 200000000000000, 1000000000000000, 4, 12, 100, 3, FALSE, TRUE, FALSE, 
     'Gold tier - Higher API limits', TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('tier-vip4', 'VIP4', 1000000000000000, 5000000000000000, 2, 8, 200, 2, TRUE, TRUE, FALSE, 
     'Platinum tier - Dedicated account manager', TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('tier-vip5', 'VIP5', 5000000000000000, 9223372036854775807, 0, 5, 500, 1, TRUE, TRUE, TRUE, 
     'Diamond tier - Zero maker fees, custom solutions', TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000)
ON CONFLICT (tier) DO NOTHING;

-- 6. Insert default tiered fee configs for common symbols
-- BTC-USDC
INSERT INTO tiered_fee_config (id, symbol, tier, maker_fee_bps, taker_fee_bps, min_fee, active, created_at, updated_at)
VALUES 
    ('fee-btc-vip0', 'BTC-USDC', 'VIP0', 10, 20, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('fee-btc-vip1', 'BTC-USDC', 'VIP1', 8, 18, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('fee-btc-vip2', 'BTC-USDC', 'VIP2', 6, 15, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('fee-btc-vip3', 'BTC-USDC', 'VIP3', 4, 12, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('fee-btc-vip4', 'BTC-USDC', 'VIP4', 2, 8, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('fee-btc-vip5', 'BTC-USDC', 'VIP5', 0, 5, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000)
ON CONFLICT (symbol, tier) DO NOTHING;

-- ETH-USDC
INSERT INTO tiered_fee_config (id, symbol, tier, maker_fee_bps, taker_fee_bps, min_fee, active, created_at, updated_at)
VALUES 
    ('fee-eth-vip0', 'ETH-USDC', 'VIP0', 10, 20, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('fee-eth-vip1', 'ETH-USDC', 'VIP1', 8, 18, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('fee-eth-vip2', 'ETH-USDC', 'VIP2', 6, 15, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('fee-eth-vip3', 'ETH-USDC', 'VIP3', 4, 12, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('fee-eth-vip4', 'ETH-USDC', 'VIP4', 2, 8, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('fee-eth-vip5', 'ETH-USDC', 'VIP5', 0, 5, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000)
ON CONFLICT (symbol, tier) DO NOTHING;

-- SOL-USDC
INSERT INTO tiered_fee_config (id, symbol, tier, maker_fee_bps, taker_fee_bps, min_fee, active, created_at, updated_at)
VALUES 
    ('fee-sol-vip0', 'SOL-USDC', 'VIP0', 10, 20, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('fee-sol-vip1', 'SOL-USDC', 'VIP1', 8, 18, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('fee-sol-vip2', 'SOL-USDC', 'VIP2', 6, 15, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('fee-sol-vip3', 'SOL-USDC', 'VIP3', 4, 12, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('fee-sol-vip4', 'SOL-USDC', 'VIP4', 2, 8, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000),
    ('fee-sol-vip5', 'SOL-USDC', 'VIP5', 0, 5, 0.01, TRUE, EXTRACT(EPOCH FROM NOW()) * 1000, EXTRACT(EPOCH FROM NOW()) * 1000)
ON CONFLICT (symbol, tier) DO NOTHING;

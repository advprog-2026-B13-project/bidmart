-- Add wallet and payment schema updates

ALTER TABLE IF EXISTS wallet
    DROP COLUMN IF EXISTS sample_column;

ALTER TABLE IF EXISTS wallet
    ADD COLUMN IF NOT EXISTS user_id UUID,
    ADD COLUMN IF NOT EXISTS available_balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS held_balance NUMERIC(19, 2) NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS idx_wallet_user_id_unique ON wallet (user_id);

ALTER TABLE IF EXISTS wallet
    ADD CONSTRAINT chk_wallet_available_non_negative CHECK (available_balance >= 0),
    ADD CONSTRAINT chk_wallet_held_non_negative CHECK (held_balance >= 0);

CREATE TABLE IF NOT EXISTS wallet_transactions (
    id UUID NOT NULL PRIMARY KEY,
    wallet_id UUID NOT NULL,
    type VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    reference_id UUID,
    created_at TIMESTAMP,
    CONSTRAINT fk_wallet_transactions_wallet
        FOREIGN KEY (wallet_id)
        REFERENCES wallet (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_wallet_transactions_type CHECK (type IN ('TOP_UP', 'WITHDRAW', 'HOLD', 'RELEASE', 'PAYMENT'))
);

CREATE INDEX IF NOT EXISTS idx_wallet_transactions_wallet_id ON wallet_transactions (wallet_id);

CREATE TABLE IF NOT EXISTS payments (
    id UUID NOT NULL PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments (user_id);

CREATE TABLE IF NOT EXISTS bids (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    amount BIGINT NOT NULL, -- stored in cents
    idempotency_key VARCHAR(512) NOT NULL UNIQUE,
    placed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bids_item_id_amount ON bids (item_id, amount DESC);

CREATE INDEX IF NOT EXISTS idx_bids_idempotency_key ON bids (idempotency_key);

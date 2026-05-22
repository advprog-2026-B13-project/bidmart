ALTER TABLE bids
    ADD COLUMN IF NOT EXISTS max_amount NUMERIC(19, 2);

UPDATE bids
SET max_amount = amount
WHERE max_amount IS NULL;

ALTER TABLE bids
    ADD COLUMN IF NOT EXISTS source VARCHAR(16);

UPDATE bids
SET source = 'MANUAL'
WHERE source IS NULL;

ALTER TABLE bids
    ALTER COLUMN source SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_bids_source'
    ) THEN
        ALTER TABLE bids
            ADD CONSTRAINT chk_bids_source CHECK (source IN ('MANUAL', 'PROXY'));
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_bids_listing_bidder_max_amount
    ON bids (listing_id, bidder_id, max_amount DESC);

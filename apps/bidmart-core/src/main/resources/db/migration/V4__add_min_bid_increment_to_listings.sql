-- Add missing column required by Listing entity.
-- Existing rows are backfilled with a sane default.
ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS min_bid_increment NUMERIC(38, 2);

UPDATE listings
SET min_bid_increment = 1.00
WHERE min_bid_increment IS NULL;

ALTER TABLE listings
    ALTER COLUMN min_bid_increment SET NOT NULL;

ALTER TABLE listings
    ALTER COLUMN min_bid_increment SET DEFAULT 1.00;

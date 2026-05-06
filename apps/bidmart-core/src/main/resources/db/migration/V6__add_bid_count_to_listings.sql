-- Add bid_count column to listings table
ALTER TABLE listings ADD COLUMN bid_count INTEGER NOT NULL DEFAULT 0;
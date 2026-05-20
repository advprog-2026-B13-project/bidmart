-- Profiling seed data for BidMart bidding load test
-- Run this AFTER you have:
--   1. Started postgres + redis (docker compose up postgres redis -d)
--   2. Started the app once so Flyway runs all migrations
--   3. Registered two accounts via POST /api/auth/register (or Swagger)
--      - seller@test.com / bidder@test.com
--   4. Filled in the UUIDs below from the users table

-- Step 1: fill in your user IDs from SELECT id, email FROM users;
\set seller_id  '6361e34f-0704-40a2-a96e-8a640c4b6d1d'
\set bidder_id  'e359c4c5-482f-4887-bb98-9f84629b7678'

-- Step 2: ensure wallets exist with enough balance
INSERT INTO wallet (id, user_id, available_balance, held_balance)
VALUES
    (gen_random_uuid(), :'seller_id'::uuid, 100000000, 0),
    (gen_random_uuid(), :'bidder_id'::uuid, 100000000, 0)
ON CONFLICT (user_id) DO UPDATE
    SET available_balance = 100000000, held_balance = 0;

-- Step 3: ensure a category exists
INSERT INTO categories (id, name) VALUES (1, 'Electronics') ON CONFLICT DO NOTHING;

-- Step 4: create 1 hot listing owned by seller, active for the next 2 hours
INSERT INTO listings (
    id, seller_id, category_id, title, description,
    starting_price, reserve_price, current_price, min_bid_increment,
    start_time, end_time, status, winner_id, created_at, updated_at
) VALUES (
    gen_random_uuid(),
    :'seller_id'::uuid,
    1,
    'Profiling Test Item',
    'Load test listing — do not use in prod',
    1000,    -- starting price (Rp 1,000)
    50000,   -- reserve price
    1000,    -- current price (same as starting)
    100,     -- min bid increment (Rp 100)
    NOW(),
    NOW() + INTERVAL '2 hours',
    'ACTIVE',
    NULL,
    NOW(),
    NOW()
) RETURNING id;
-- Copy the returned listing ID — you need it in the k6 script as LISTING_ID

-- Step 5: verify
SELECT l.id AS listing_id, l.status, l.current_price,
       w_s.available_balance AS seller_balance,
       w_b.available_balance AS bidder_balance
FROM listings l
JOIN wallet w_s ON w_s.user_id = :'seller_id'::uuid
JOIN wallet w_b ON w_b.user_id = :'bidder_id'::uuid
WHERE l.title = 'Profiling Test Item';

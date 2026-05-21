# BidMart Bidding — Load Testing & Profiling

## Goal

Prove that **PostgreSQL writes are the bottleneck** in the `placeBid` path, not the Java/Spring layer or service topology. This justifies the monolith-first architecture and motivates the async PSQL offload refactor (Redis Streams).

---

## Setup

| Component | Value |
|---|---|
| Tool | [k6](https://k6.io) |
| Script | `profiling/k6-bid-load.js` |
| Seed data | `profiling/seed.sql` |
| App | `bidmart-core` (Spring Boot, single instance) |
| Infra | Docker — PostgreSQL + Redis |
| Profile | `dev` (mock wallet, no Midtrans) |

### k6 Config

```
VUs: 1 (sequential bids — every bid beats the last, exercises full LEADING path)
Iterations: 300
Thresholds: place_bid_latency p(95) < 500ms, bid_errors < 5
```

---

## How to Run

### 1. Start infrastructure

```powershell
docker compose up postgres redis -d
```

### 2. Start the app

```powershell
# From apps/bidmart-core
$env:SPRING_PROFILES_ACTIVE="dev"; ./gradlew bootRun
```

### 3. Seed the database (first time only)

```powershell
docker exec -i bidmart-postgres psql -U admin -d bidmart_db < profiling/seed.sql
```

Seed creates:
- Seller: `6361e34f-0704-40a2-a96e-8a640c4b6d1d`
- Bidder: `e359c4c5-482f-4887-bb98-9f84629b7678` (email: `buyer@test.com`, pass: `password123`)
- Listing: `39a594cf-da13-4df3-b697-cb1c133e12ce` (starting price 1000, min increment 100)

If accounts aren't active yet:
```powershell
docker exec bidmart-postgres psql -U admin -d bidmart_db -c "UPDATE users SET status = 'ACTIVE' WHERE email IN ('seller@test.com', 'buyer@test.com');"
```

### 4. Run load test (automated — seeds + k6 + captures Prometheus in one step)

```powershell
# Baseline
./profiling/run-loadtest.ps1

# After refactor
./profiling/run-loadtest.ps1 -Export profiling/after-summary.json -Prometheus profiling/after-prometheus.txt
```

The script:
1. Resets the listing to ACTIVE with a fresh 2-hour window
2. Flushes the Redis cache for that listing so the app re-caches the reset state
3. Runs k6
4. Captures Prometheus per-stage metrics to a file

**Manual steps (if you need to run separately):**

```powershell
# Seed only
docker exec -i bidmart-postgres psql -U admin -d bidmart_db --file=/dev/stdin < profiling/seed.sql

# k6 only
k6 run --summary-export=profiling/baseline-summary.json profiling/k6-bid-load.js

# Prometheus capture only
curl.exe -s http://localhost:8080/actuator/prometheus | Select-String "bidding_" | Where-Object { $_ -notmatch "^#" } | Out-File profiling/baseline-prometheus.txt
```

### 6. Attach IntelliJ profiler (optional, for flame graph)

With `bootRun` running in PowerShell:
- IntelliJ → **Run > Attach Profiler to Process** → pick `BidmartCoreApplication`
- Start recording → run k6 → stop recording → open flame graph

---

## Baseline Results (2026-05-20)

### k6 HTTP Latency — `place_bid_latency` (end-to-end, 300 iterations)

| Metric | Value |
|---|---|
| avg | 45.38 ms |
| median | 42.87 ms |
| p(90) | 63.02 ms |
| **p(95)** | **70.76 ms** |
| max | 160.96 ms |
| errors | 0 / 300 |

Raw k6 export: `profiling/baseline-summary.json`

---

### Micrometer Per-Stage Breakdown (Prometheus scrape, same run)

Derived from `sum / count` per timer. Source: `/actuator/prometheus`.

| Stage | avg latency | % of total |
|---|---|---|
| `bidding.db_write` | 11.91 ms | **53.7%** |
| `bidding.listing_read` | 3.65 ms | 16.5% |
| `bidding.wallet_hold` | 2.06 ms | 9.3% |
| `bidding.redis_decision` | 1.97 ms | 8.9% |
| *(unaccounted — overhead, serialization)* | ~2.56 ms | 11.6% |
| **`bidding.place_bid` (total)** | **22.16 ms** | 100% |

Raw Prometheus values:

```
bidding_db_write_seconds_count{outcome="leading"}    300
bidding_db_write_seconds_sum{outcome="leading"}      3.5717861   → avg 11.91ms

bidding_listing_read_seconds_count                   300
bidding_listing_read_seconds_sum                     1.0935428   → avg 3.65ms

bidding_wallet_hold_seconds_count                    300
bidding_wallet_hold_seconds_sum                      0.6185051   → avg 2.06ms

bidding_redis_decision_seconds_count                 300
bidding_redis_decision_seconds_sum                   0.5910856   → avg 1.97ms

bidding_place_bid_seconds_count{outcome="leading"}   300
bidding_place_bid_seconds_sum{outcome="leading"}     6.6491741   → avg 22.16ms
```

---

## Conclusion

PSQL accounts for **~70% of `placeBid` latency** (`db_write` 54% + `listing_read` 16%).  
Redis is only **9%** — the atomic Lua script adds negligible overhead.

The bottleneck is I/O to PostgreSQL, not the Java application layer or the monolith topology. Splitting `bidding` into a separate service would add network hops without reducing DB call count.

**Target for async refactor**: move `db_write` off the request path via Redis Streams (XADD + sequential consumer). Expected improvement: ≥50% reduction in `place_bid` avg latency.

---

## After-Refactor Run (2026-05-20)

**Optimizations applied:**
- Cache-first listing read: `ListingCatalogAdapter` reads from Redis hash before hitting PostgreSQL. Redis hash now stores all 10 static fields (added `sellerId`, `startingPrice`, `reservePrice`, `minBidIncrement`, `startTime`).
- COUNT query: replaced `findByListing().size()` full table scan with `countByListingId` (`SELECT COUNT(*)`).

### k6 HTTP Latency — `place_bid_latency` (300 iterations, 0 errors)

| Metric | Baseline | After | Delta |
|---|---|---|---|
| avg | 48.67 ms | **35.85 ms** | -26% |
| median | 44.31 ms | **31.69 ms** | -28% |
| p(90) | 70.55 ms | **46.79 ms** | -34% |
| **p(95)** | **86.92 ms** | **66.08 ms** | **-24%** |
| max | 214.09 ms | **148.97 ms** | -30% |
| errors | 0 / 300 | **0 / 300** | — |

Raw k6 export: `profiling/after-summary.json`

---

### Micrometer Per-Stage Breakdown

| Stage | Baseline avg | After avg | Delta | Baseline % | After % |
|---|---|---|---|---|---|
| `bidding.db_write` | 11.91 ms | **9.65 ms** | -19% | 53.7% | **57.9%** |
| `bidding.listing_read` | 3.65 ms | **1.31 ms** | **-64%** | 16.5% | **7.9%** |
| `bidding.wallet_hold` | 2.06 ms | 1.77 ms | -14% | 9.3% | 10.6% |
| `bidding.redis_decision` | 1.97 ms | 1.37 ms | -31% | 8.9% | 8.2% |
| **`bidding.place_bid`** | **22.16 ms** | **16.67 ms** | **-25%** | 100% | 100% |

Raw Prometheus values:

```
bidding_db_write_seconds_count{outcome="leading"}    300
bidding_db_write_seconds_sum{outcome="leading"}      2.8955289   → avg 9.65ms

bidding_listing_read_seconds_count                   300
bidding_listing_read_seconds_sum                     0.3940437   → avg 1.31ms

bidding_wallet_hold_seconds_count                    300
bidding_wallet_hold_seconds_sum                      0.5302746   → avg 1.77ms

bidding_redis_decision_seconds_count                 300
bidding_redis_decision_seconds_sum                   0.4104915   → avg 1.37ms

bidding_place_bid_seconds_count{outcome="leading"}   300
bidding_place_bid_seconds_sum{outcome="leading"}     5.0019886   → avg 16.67ms
```

---

## Updated Conclusion

`listing_read` dropped **64%** (3.65 ms → 1.31 ms) — cache-first eliminated the PSQL round-trip for warm auctions. End-to-end `place_bid` improved **25%** (22.16 ms → 16.67 ms).

`db_write` is now the sole dominant stage at **58% of total latency**, confirming it as the next optimization target. The result validates the hypothesis: PSQL I/O is the bottleneck, not the Java layer or monolith topology.


---

## Staging Deployment Results (2026-05-21)

Both runs executed against `https://api.staging.bidmart.store` — same k6 script, same 300 iterations, 1 VU.

### k6 HTTP Latency — `place_bid_latency`

| Metric | Baseline (before deploy) | After deploy | Delta |
|---|---|---|---|
| avg | 150.08 ms | **37.16 ms** | **-75%** |
| median | 98.94 ms | **33.71 ms** | -66% |
| p(90) | 300.77 ms | **48.61 ms** | -84% |
| **p(95)** | **393.26 ms** | **54.80 ms** | **-86%** |
| max | 2398.93 ms | **392.70 ms** | -84% |
| errors | 0 / 300 | **0 / 300** | — |

> Baseline: unoptimised build, warm server. After: fresh restart + HTTP warmup (20 × GET /api/catalog/listings).

Raw k6 exports: `profiling/k6/baseline-staging.json`, `profiling/k6/after-staging.json`

---

### Micrometer Per-Stage Breakdown — Staging (Clean Delta Run)

Computed as `(post − pre) / 300` from two Prometheus scrapes bracketing a 300-bid k6 run on a fully-warm JVM. Source files: `after-staging-pre2.txt` → `after-staging-prometheus-after.txt`.

| Stage | Baseline avg | After avg | Delta | Baseline % | After % |
|---|---|---|---|---|---|
| `bidding.db_write` | 5.16 ms | **3.96 ms** | **-23%** | 49.8% | **48.9%** |
| `bidding.listing_read` | 1.43 ms | **0.44 ms** | **-69%** | 13.8% | **5.5%** |
| `bidding.wallet_hold` | 1.26 ms | **0.79 ms** | **-37%** | 12.1% | **9.8%** |
| `bidding.redis_decision` | 1.08 ms | **0.69 ms** | **-36%** | 10.4% | **8.5%** |
| *(unaccounted — overhead)* | ~1.44 ms | ~2.21 ms | — | 13.9% | 27.3% |
| **`bidding.place_bid` (total)** | **10.37 ms** | **8.09 ms** | **-22%** | 100% | 100% |

Raw Prometheus snapshots can be found in google drive: `after-staging-pre2.txt`, `after-staging-prometheus-after.txt`

### Key Findings on Staging

- **`listing_read` dropped 69%** (1.43 ms → 0.44 ms) — Redis cache-first eliminated the PSQL round-trip; confirmed on the real deployed environment with a clean warm-JVM delta measurement.
- **`place_bid` total dropped 22%** (10.37 ms → 8.09 ms) — end-to-end improvement on staging.
- **k6 p(95) dropped 86%** (393 ms → 54.8 ms) end-to-end.
- Zero bid errors across all runs.

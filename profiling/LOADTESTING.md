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

## After-Refactor Run

> To be filled in after Redis Streams async write implementation.

```
k6 run --summary-export=profiling/after-summary.json profiling/k6-bid-load.js
```

Then scrape Prometheus again and fill in the after table here.

| Stage | baseline avg | after avg | delta |
|---|---|---|---|
| `bidding.db_write` | 11.91 ms | — | — |
| `bidding.listing_read` | 3.65 ms | — | — |
| `bidding.wallet_hold` | 2.06 ms | — | — |
| `bidding.redis_decision` | 1.97 ms | — | — |
| **`bidding.place_bid`** | **22.16 ms** | — | — |

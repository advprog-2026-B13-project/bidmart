/**
 * BidMart bidding load test — baseline profiling
 *
 * SETUP (one-time, do this before running the script):
 *
 *   1. Start infra + app:
 *        docker compose up postgres redis -d
 *        (in apps/bidmart-core) $env:SPRING_PROFILES_ACTIVE="dev"; ./gradlew bootRun
 *
 *   2. Register two accounts via Postman (import postman/BidMart-API.postman_collection.json):
 *        POST /api/auth/register  { email, password, displayName }
 *      Do this for both SELLER_EMAIL and BIDDER_EMAIL below.
 *
 *   3. Bypass email OTP (no mail server in dev) — run in psql:
 *        UPDATE users SET status = 'ACTIVE' WHERE email IN ('seller@test.com', 'bidder@test.com');
 *        (connect with: docker exec -it bidmart-postgres psql -U admin -d bidmart_db)
 *
 *   4. Create a test listing:
 *        Login as seller via Postman → POST /api/catalog/listings (fill in your listing body)
 *        Copy the returned listing id into LISTING_ID below.
 *        OR run profiling/seed.sql after filling in seller_id from: SELECT id FROM users WHERE email='seller@test.com';
 *
 *   5. Top up bidder wallet:
 *        POST /api/wallet/top-up  { amount: 999999999 }  (logged in as bidder via Postman)
 *
 *   6. Fill in the three constants below, then run:
 *        k6 run --summary-export=profiling/baseline-summary.json profiling/k6-bid-load.js
 *
 * After refactor, run again:
 *        k6 run --summary-export=profiling/after-summary.json profiling/k6-bid-load.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// ─── FILL THESE IN ────────────────────────────────────────────────────────────
const BASE_URL      = 'http://localhost:8080';
const LISTING_ID    = '39a594cf-da13-4df3-b697-cb1c133e12ce';
const BIDDER_EMAIL  = 'buyer@test.com';
const BIDDER_PASS   = 'password123';
// ─────────────────────────────────────────────────────────────────────────────

const placeBidLatency = new Trend('place_bid_latency', true);
const bidErrors       = new Counter('bid_errors');

export const options = {
    // 1 VU: sequential bids, every bid beats the last — exercises the full LEADING path
    // Change to stages with more VUs after getting clean baseline db_write numbers
    vus: 1,
    iterations: 300,
    thresholds: {
        'place_bid_latency': ['p(95)<500'],
        'bid_errors': ['count<5'],
    },
};

// setup() runs once before VUs start — login and return the token
export function setup() {
    const res = http.post(`${BASE_URL}/api/auth/login`,
        JSON.stringify({ email: BIDDER_EMAIL, password: BIDDER_PASS }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    if (res.status !== 200) {
        throw new Error(`Login failed: ${res.status} — ${res.body}`);
    }

    const body = JSON.parse(res.body);
    const token = body.data && body.data.accessToken;
    if (!token) {
        throw new Error(`No accessToken in login response: ${res.body}`);
    }

    console.log(`Logged in as ${BIDDER_EMAIL}, token length: ${token.length}`);
    return { token };
}

// module-level counter — persists across iterations for the same VU
let nextAmount = 1200;

// Each VU receives the token via the `data` parameter
export default function (data) {
    const authHeaders = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${data.token}`,
        },
    };

    const amount = nextAmount;
    nextAmount += 200; // each bid 200 above the last (well above 100 min increment)

    const payload = JSON.stringify({
        listingId: LISTING_ID,
        amount: amount,
        bidType: 'MANUAL',
    });

    const res = http.post(`${BASE_URL}/api/bidding/bids`, payload, authHeaders);

    placeBidLatency.add(res.timings.duration);

    const ok = check(res, {
        'status 200': (r) => r.status === 200,
        'bid accepted or outbid': (r) => {
            try {
                const b = JSON.parse(r.body);
                return b.data && (b.data.status === 'ACCEPTED' || b.data.status === 'OUTBID');
            } catch { return false; }
        },
    });

    if (!ok) {
        bidErrors.add(1);
        console.log(`[VU=${__VU} ITER=${__ITER} amount=${amount}] ${res.status}: ${res.body}`);
    }

    sleep(0.1);
}

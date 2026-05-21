/**
 * BidMart bidding load test — baseline profiling
 *
 * Run via the automated script (handles setup automatically):
 *   ./profiling/run-loadtest.ps1
 *
 * Or manually:
 *   k6 run --summary-export=profiling/baseline-summary.json profiling/k6-bid-load.js
 *
 * After refactor:
 *   ./profiling/run-loadtest.ps1 -Export profiling/after-summary.json -Prometheus profiling/after-prometheus.txt
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const BASE_URL     = __ENV.BASE_URL     || 'http://localhost:8080';
const LISTING_ID   = __ENV.LISTING_ID   || '39a594cf-da13-4df3-b697-cb1c133e12ce';
const BIDDER_EMAIL = __ENV.BIDDER_EMAIL || 'buyer@test.com';
const BIDDER_PASS  = __ENV.BIDDER_PASS  || 'password123';

const placeBidLatency = new Trend('place_bid_latency', true);
const bidErrors       = new Counter('bid_errors');

export const options = {
    // 1 VU: sequential bids, every bid beats the last — exercises the full LEADING path
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

    console.log(`Logged in as ${BIDDER_EMAIL} against listing ${LISTING_ID}`);
    return { token };
}

// module-level counter — persists across iterations for the same VU
let nextAmount = 1200;

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

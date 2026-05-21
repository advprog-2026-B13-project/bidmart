param(
    [string]$BaseUrl   = "http://localhost:8080",
    [string]$Export    = "$PSScriptRoot/baseline-summary.json",
    [string]$Prometheus = "$PSScriptRoot/baseline-prometheus.txt"
)

$ErrorActionPreference = "Stop"
$headers = @{ "Content-Type" = "application/json" }

# ── helpers ──────────────────────────────────────────────────────────────────

function Invoke-Api($Method, $Path, $Body, $Token) {
    $h = @{ "Content-Type" = "application/json" }
    if ($Token) { $h["Authorization"] = "Bearer $Token" }
    $resp = Invoke-WebRequest -Uri "$BaseUrl$Path" -Method $Method `
        -Headers $h -Body ($Body | ConvertTo-Json) -UseBasicParsing
    return ($resp.Content | ConvertFrom-Json)
}

function Psql($Sql) {
    $result = $Sql | docker exec -i bidmart-postgres psql -U admin -d bidmart_db -t -A
    return $result.Trim()
}

# ── step 1: register or reuse seller ─────────────────────────────────────────

Write-Host "`n==> Registering seller..." -ForegroundColor Cyan
try {
    Invoke-Api POST /api/auth/register @{
        email       = "profseller@test.com"
        password    = "password123"
        displayName = "Profiling Seller"
    } | Out-Null
    Write-Host "    seller registered"
} catch {
    Write-Host "    seller already exists (skipping)"
}

Write-Host "`n==> Registering bidder..." -ForegroundColor Cyan
try {
    Invoke-Api POST /api/auth/register @{
        email       = "profbuyer@test.com"
        password    = "password123"
        displayName = "Profiling Buyer"
    } | Out-Null
    Write-Host "    bidder registered"
} catch {
    Write-Host "    bidder already exists (skipping)"
}

# ── step 2: activate both accounts via DB ────────────────────────────────────

Write-Host "`n==> Activating accounts in DB..." -ForegroundColor Cyan
Psql "UPDATE users SET status = 'ACTIVE' WHERE email IN ('profseller@test.com', 'profbuyer@test.com');" | Out-Null

$sellerId = Psql "SELECT id FROM users WHERE email = 'profseller@test.com';"
$buyerId  = Psql "SELECT id FROM users WHERE email = 'profbuyer@test.com';"
Write-Host "    seller=$sellerId"
Write-Host "    buyer=$buyerId"

# ── step 3: top up wallets ───────────────────────────────────────────────────

Write-Host "`n==> Resetting wallets..." -ForegroundColor Cyan
Psql @"
INSERT INTO wallet (id, user_id, available_balance, held_balance)
VALUES (gen_random_uuid(), '$sellerId'::uuid, 100000000, 0),
       (gen_random_uuid(), '$buyerId'::uuid,  100000000, 0)
ON CONFLICT (user_id) DO UPDATE SET available_balance = 100000000, held_balance = 0;
"@ | Out-Null
Write-Host "    wallets OK"

# ── step 4: login as seller ───────────────────────────────────────────────────

Write-Host "`n==> Logging in as seller..." -ForegroundColor Cyan
$sellerLogin = Invoke-Api POST /api/auth/login @{
    email    = "profseller@test.com"
    password = "password123"
}
$sellerToken = $sellerLogin.data.accessToken
Write-Host "    token OK (length $($sellerToken.Length))"

# ── step 5: ensure category exists ───────────────────────────────────────────

Psql "INSERT INTO categories (id, name) VALUES (1, 'Electronics') ON CONFLICT DO NOTHING;" | Out-Null

# ── step 6: create listing via API ───────────────────────────────────────────

Write-Host "`n==> Creating listing..." -ForegroundColor Cyan
$startTime = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
$endTime   = (Get-Date).AddHours(2).ToString("yyyy-MM-ddTHH:mm:ss")

$listing = Invoke-Api POST /api/catalog/listings/create @{
    categoryId      = 1
    title           = "Profiling Test Item"
    description     = "Load test listing - do not use in prod"
    startingPrice   = 1000
    reservePrice    = 50000
    minBidIncrement = 100
    startTime       = $startTime
    endTime         = $endTime
} -Token $sellerToken

$listingId = $listing.id
Write-Host "    listingId=$listingId"

# ── step 7: activate listing via DB (bypass time checks) ─────────────────────

Write-Host "`n==> Activating listing in DB..." -ForegroundColor Cyan
Psql @"
UPDATE listings SET status = 'ACTIVE' WHERE id = '$listingId'::uuid;
"@ | Out-Null

# ── step 8: flush Redis cache so app picks up fresh state ────────────────────

Write-Host "`n==> Flushing Redis auction cache..." -ForegroundColor Cyan
docker exec bidmart-redis redis-cli DEL "auction:${listingId}:state" | Out-Null
docker exec bidmart-redis redis-cli ZREM "auction:expiry" $listingId | Out-Null

# ── step 9: write listing ID into k6 env file ────────────────────────────────

$envFile = "$PSScriptRoot/.k6env"
"LISTING_ID=$listingId`nBIDDER_EMAIL=profbuyer@test.com`nBIDDER_PASS=password123" | Out-File $envFile -Encoding utf8
Write-Host "`n==> k6 env written to $envFile"

# ── step 10: run k6 ──────────────────────────────────────────────────────────

Write-Host "`n==> Running k6 load test..." -ForegroundColor Cyan
$env:LISTING_ID    = $listingId
$env:BIDDER_EMAIL  = "profbuyer@test.com"
$env:BIDDER_PASS   = "password123"

k6 run --summary-export=$Export `
       -e LISTING_ID=$listingId `
       -e BIDDER_EMAIL=profbuyer@test.com `
       -e BIDDER_PASS=password123 `
       "$PSScriptRoot/k6-bid-load.js"

# ── step 11: capture Prometheus ───────────────────────────────────────────────

Write-Host "`n==> Capturing Prometheus metrics..." -ForegroundColor Cyan
curl.exe -s "$BaseUrl/actuator/prometheus" |
    Select-String "bidding_" |
    Where-Object { $_ -notmatch "^#" } |
    Out-File $Prometheus -Encoding utf8

Write-Host "`n==> Done." -ForegroundColor Green
Write-Host "    k6 export : $Export"
Write-Host "    prometheus: $Prometheus"
Write-Host "    listing ID: $listingId"

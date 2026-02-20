# BidMart 🔨

## Struktur Repositori (Monorepo)

```text
bidmart/
├── apps/
│   ├── bidmart-fe/       # Aplikasi klien interaktif (Next.js + Tailwind)
│   ├── bidmart-core/     # Modul Auth, Katalog, Dompet, dan Notifikasi (Java Spring Boot)
│   └── bidmart-auction/  # Modul proses pelelangan real-time (Rust Axum)
├── infra/                # Konfigurasi Docker Compose, NGINX, dan skrip inisialisasi DB
├── .github/              # CI/CD Workflows (GitHub Actions)
└── README.md
```

## Local Development

### 1. Start Database
```bash
docker compose -f infra/dev-compose.yml up -d
```

### 2. Run Services
```bash
# Core (port 8080)
cd apps/bidmart-core && ./gradlew bootRun

# Auction (port 8081)
cd apps/bidmart-auction && cargo run

# Frontend (port 3000)
cd apps/bidmart-fe && pnpm dev
```

### 3. Test Chained Health Check
Buka http://localhost:3000/debug di browser.

Halaman ini menampilkan status koneksi berantai: **FE → Auction → Core → DB**
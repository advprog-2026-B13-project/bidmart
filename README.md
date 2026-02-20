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
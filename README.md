# BidMart 🔨

## Struktur Repositori (Monorepo)

```text
bidmart/
├── bidmart-fe/        # Aplikasi klien interaktif (Next.js)
├── bidmart-core/      # Modul Auth, Katalog, Dompet, dan Notifikasi (Java)
├── bidmart-auction/   # Modul proses pelelangan real-time (Rust)
├── infra/             # Konfigurasi Docker Compose, NGINX, dan skrip inisialisasi DB
└── README.md
```
# BidMart Staging Infrastructure

Docker Compose for staging: PostgreSQL + Redis + BidMart Core API.

## Prerequisites

- Docker & Docker Compose v2
- [Resend API key](https://resend.com) (optional — leave empty to disable email OTP)

## Quick Start

```bash
# 1. Copy and fill environment variables
cp staging.env.example staging.env
nano staging.env   # fill in DATABASE_PASSWORD, AUTH_JWT_SECRET, FRONTEND_URL

# 2. Start all services
docker compose -f staging-docker-compose.yml --env-file staging.env up -d

# 3. Check health
docker compose -f staging-docker-compose.yml --env-file staging.env ps

# 4. Watch core logs
docker compose -f staging-docker-compose.yml --env-file staging.env logs -f core
```

## Environment Variables

| Variable | Required | Description |
|---|---|---|
| `AUTH_JWT_SECRET` | Yes | 256-bit secret key for HS256 JWT signing |
| `FRONTEND_URL` | Yes | Public frontend URL (e.g. `https://bidmart.store`) |
| `RESEND_API_KEY` | No | Resend API key for email OTP. Leave empty to disable email MFA |
| `MAIL_FROM` | No | Sender email for Resend. Default: `no-reply@bidmart.store` |

> PostgreSQL password is baked into the compose file for internal use between containers.

## Services

| Service | Port | Description |
|---|---|---|
| `postgres` | 5432 | PostgreSQL 16 |
| `redis` | 6379 | Redis 8 |
| `core` | 8080 | BidMart Spring Boot API |

## Generate a JWT Secret

```bash
openssl rand -hex 32
```

## Stop

```bash
docker compose -f staging-docker-compose.yml --env-file staging.env down
```

## Rebuild Core (after code changes)

```bash
docker compose -f staging-docker-compose.yml --env-file staging.env up -d --build core
```

## Notes

- Core waits for postgres and redis to be healthy before starting (`depends_on` + `condition: service_healthy`)
- PostgreSQL data persists to a named volume (`postgres_data`) — survives restarts
- Redis data persists to a named volume (`redis_data`)
- Email MFA/OTP will silently fail if `RESEND_API_KEY` is empty — users won't receive codes, but can still log in if already verified

# Bidmart Auction Service

Auction service built with Rust and Axum.

## Overview

Backend service for auction operations.

**Technology Stack:**
- Language: Rust
- Framework: Axum (web framework)
- Runtime: Tokio (async runtime)
- Serialization: Serde + Serde JSON

## Prerequisites

- Rust 1.70+ (stable channel)
- Cargo (comes with Rust)

## Installation

1. Ensure Rust is installed:
```bash
rustc --version
cargo --version
```

2. Navigate to the service directory:
```bash
cd apps/bidmart-auction
```

## Running

### Development server
```bash
cargo run
```

The server starts on `http://localhost:3000`

### Build for production
```bash
cargo build --release
```

Binary will be at `target/release/bidmart-auction`

## Testing

Run all tests:
```bash
cargo test
```

Run tests with output:
```bash
cargo test -- --nocapture
```

## Code Quality

Check code formatting:
```bash
cargo fmt -- --check
```

Fix formatting:
```bash
cargo fmt
```

Run linter:
```bash
cargo clippy -- -D warnings
```


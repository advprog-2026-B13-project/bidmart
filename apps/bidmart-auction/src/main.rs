//! BidMart Auction Service: Main entry point.
//!
//! Architecture: Clean Architecture + Hexagonal (Ports & Adapters)
//! - adapter/http: Routes, handlers, HTTP-specific logic
//! - app: Use cases, DTOs, application errors
//! - domain: Entities, types, domain errors
//! - port: Trait definitions (interfaces)

mod adapter;
mod app;
mod domain;
mod port;

use std::sync::Arc;

use adapter::http::routes::{create_router, AppState};
use app::place_bid::PlaceBidUseCase;

use crate::port::BidRepository;
use crate::adapter::repository::PlaceholderBidRepository;

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt::init();

    // TODO: Initialize actual infrastructure (DB, Redis, etc.)
    let bid_repo: Arc<dyn BidRepository> = Arc::new(PlaceholderBidRepository::new());

    // Create use case with injected dependencies
    let place_bid = Arc::new(PlaceBidUseCase::new(bid_repo));

    // Build app state
    let state = AppState {
        place_bid: place_bid.clone(),
    };

    // Create router
    let app = create_router(state);

    let addr = std::net::SocketAddr::from(([127, 0, 0, 1], 8081));
    let listener = tokio::net::TcpListener::bind(&addr)
        .await
        .expect("Failed to bind to port 8081");

    tracing::info!("Server running at http://{}", addr);

    axum::serve(listener, app).await.expect("Server error");
}

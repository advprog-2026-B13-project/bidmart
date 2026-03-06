//! BidMart Auction Service: Main entry point.
//!
//! Architecture: Clean Architecture + Hexagonal (Ports & Adapters)
//! - adapter/http: Routes, handlers, HTTP-specific logic
//! - app: Use cases, DTOs, application errors
//! - domain: Entities, types, domain errors
//! - port: Trait definitions (interfaces)

mod adapter;
mod app;
mod config;
mod domain;
mod port;

use std::net::SocketAddr;
use std::sync::Arc;

use adapter::http::routes::{create_router, AppState};
use app::place_bid::PlaceBidUseCase;

use crate::adapter::repository::{PostgresBidRepository, PostgresListingRepository};
use crate::port::{BidRepository, ListingRepository};

use crate::config::app_config::AppConfig;
use crate::config::database::init_postgres_pool;

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt::init();

    let app_config = AppConfig::from_env();

    let pool = match init_postgres_pool(&app_config.database_url, 10).await {
        Ok(pool) => pool,
        Err(e) => {
            tracing::error!("Failed to initialize database pool: {}", e);
            std::process::exit(1);
        }
    };

    tracing::info!("Migrating database...");
    if let Err(e) = sqlx::migrate!().run(&pool).await {
        tracing::error!("Database migration failed: {}", e);
        std::process::exit(1);
    }
    tracing::info!("Database migrated successfully!");

    let bid_repo: Arc<dyn BidRepository> = Arc::new(PostgresBidRepository::new(pool.clone()));
    let listing_repo: Arc<dyn ListingRepository> = Arc::new(PostgresListingRepository::new(pool));
    // Create use case with injected dependencies
    let place_bid = Arc::new(PlaceBidUseCase::new(bid_repo, listing_repo.clone())); // Update constructor
    let get_highest_bid = Arc::new(app::get_highest_bid::GetHighestBidUseCase::new(
        place_bid.bid_repo.clone(),
    ));
    let register_listing = Arc::new(app::register_listing::RegisterListingUseCase::new(
        listing_repo,
    ));

    // Build app state
    let state = AppState {
        place_bid: place_bid.clone(),
        get_highest_bid: get_highest_bid.clone(),
        register_listing: register_listing.clone(),
    };

    // Create router
    let app = create_router(state);

    let addr: SocketAddr = format!("{}:{}", app_config.host, app_config.port)
        .parse()
        .expect("Invalid host or port");

    let listener = tokio::net::TcpListener::bind(&addr)
        .await
        .expect("Failed to bind to port 8081");

    tracing::info!("Server running at http://{}", addr);

    axum::serve(listener, app).await.expect("Server error");
}

//! Axum route definitions and shared application state.

use std::sync::Arc;

use axum::routing::{get, post};
use axum::Router;

use crate::app::use_case::get_highest_bid::GetHighestBidUseCase;
use crate::app::use_case::place_bid::PlaceBidUseCase;
use crate::app::use_case::register_listing::RegisterListingUseCase;

use super::handlers;

/// Shared state injected into all Axum handlers.
#[derive(Clone)]
pub struct AppState {
    pub place_bid: Arc<PlaceBidUseCase>,
    pub get_highest_bid: Arc<GetHighestBidUseCase>,
    pub register_listing: Arc<RegisterListingUseCase>,
}

/// Build the Axum router with all routes.
pub fn create_router(state: AppState) -> Router {
    Router::new()
        .route("/health", get(handlers::health_check))
        .route("/api/health/status", get(handlers::chained_health))
        .route("/api/listing/:listing_id/bids", post(handlers::place_bid))
        .route(
            "/api/listing/:listing_id/bids/highest",
            get(handlers::get_highest_bid),
        )
        .route("/api/listing/new", post(handlers::register_listing))
        .with_state(state)
}

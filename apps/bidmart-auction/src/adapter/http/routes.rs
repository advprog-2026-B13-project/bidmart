//! Axum route definitions and shared application state.

use std::sync::Arc;

use axum::routing::{get, post};
use axum::Router;

use crate::app::get_highest_bid::GetHighestBidUseCase;
use crate::app::place_bid::PlaceBidUseCase;

use super::handlers;

/// Shared state injected into all Axum handlers.
#[derive(Clone)]
pub struct AppState {
    pub place_bid: Arc<PlaceBidUseCase>,
    pub get_highest_bid: Arc<GetHighestBidUseCase>,
}

/// Build the Axum router with all routes.
pub fn create_router(state: AppState) -> Router {
    Router::new()
        .route("/health", get(handlers::health_check))
        .route("/api/health/status", get(handlers::chained_health))
        .route("/api/items/:item_id/bids", post(handlers::place_bid))
        .route("/api/items/:item_id/bids/highest", get(handlers::get_highest_bid))
        .with_state(state)
}

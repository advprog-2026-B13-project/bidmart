//! Application-layer DTOs.

use serde::{Deserialize, Serialize};

/// Input for PlaceBidUseCase.
#[derive(Debug, Deserialize)]
pub struct PlaceBidCommand {
    pub buyer_id: String,
    pub listing_id: String,
    pub bid_amount: i64,
}

/// Output from PlaceBidUseCase.
#[derive(Debug, Serialize)]
pub struct PlaceBidResult {
    pub bid_id: String,
    pub buyer_id: String,
    pub listing_id: String,
    pub bid_amount: i64,
}

pub struct GetHighestBidCommand {
    pub listing_id: String,
}
pub struct GetHighestBidResult {
    pub buyer_id: String,
    pub bid_amount: i64,
}

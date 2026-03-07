//! Application-layer DTOs.

use serde::{Deserialize, Serialize};
use uuid::Uuid;

/// Input for PlaceBidUseCase.
#[derive(Debug, Deserialize)]
pub struct PlaceBidCommand {
    pub buyer_id: Uuid,
    pub listing_id: Uuid,
    pub bid_amount: i64,
}

/// Output from PlaceBidUseCase.
#[derive(Debug, Serialize)]
pub struct PlaceBidResult {
    pub bid_id: Uuid,
    pub buyer_id: Uuid,
    pub listing_id: Uuid,
    pub bid_amount: i64,
}

pub struct GetHighestBidCommand {
    pub listing_id: Uuid,
}
pub struct GetHighestBidResult {
    pub buyer_id: Uuid,
    pub bid_amount: i64,
}

pub struct RegisterListingCommand {
    pub id: Uuid,
    pub seller_id: Uuid,
    pub starting_price: i64,
    pub start_time: chrono::DateTime<chrono::Utc>,
    pub end_time: chrono::DateTime<chrono::Utc>,
    pub minimum_increment: i64,
}

pub struct RegisterListingResult {
    pub listing_id: Uuid,
}

/// Input for DeleteListingUseCase.
#[derive(Debug, Deserialize)]
pub struct DeleteListingCommand {
    pub listing_id: Uuid,
}

/// Output from DeleteListingUseCase.
#[derive(Debug, Serialize)]
pub struct DeleteListingResult {
    pub listing_id: Uuid,
}

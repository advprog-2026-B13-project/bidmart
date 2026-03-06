//! Axum request handlers: thin glue between HTTP and use cases.

use std::result;

use axum::extract::{Path, State};
use axum::Json;
use serde::{Deserialize, Serialize};

use crate::adapter::http::error::ApiError;
use crate::adapter::http::routes::AppState;
use crate::app::dto::{GetHighestBidCommand, PlaceBidCommand};

/// POST /api/items/{id}/bids request body.
#[derive(Debug, Deserialize)]
pub struct BidRequest {
    pub user_id: String,
    pub bid_amount: i64,
}

/// Bid response returned by place_bid.
#[derive(Debug, Serialize)]
pub struct BidResponse {
    pub bid_id: String,
    pub item_id: String,
    pub user_id: String,
    pub bid_amount: i64,
}


#[derive(Debug, Serialize)]
pub struct HighestBidResponse {
    pub user_id: String,
    pub bid_amount: i64,
}



/// GET /health
pub async fn health_check() -> &'static str {
    "Auction Service is running!"
}

/// GET /api/health/status
pub async fn chained_health(
    State(_state): State<AppState>,
) -> Json<serde_json::Value> {
    Json(serde_json::json!({
        "service": "bidmart-auction",
        "status": "UP"
    }))
}

/// POST /api/items/{id}/bids
/// 
/// Requires JWT authentication (TODO: implement JWT middleware).
/// Includes idempotency mechanism to prevent duplicate bids.
pub async fn place_bid(
    State(state): State<AppState>,
    Path(item_id): Path<String>,
    Json(body): Json<BidRequest>,
) -> Result<Json<BidResponse>, ApiError> {
    // TODO: Extract user_id from JWT token, not from request body
    // For now, accept user_id from body (skeleton)
    println!("Received place bid request for item: {}", item_id);

    let cmd = PlaceBidCommand {
        user_id: body.user_id,
        item_id: item_id.clone(),
        bid_amount: body.bid_amount,
    };

    let result = state.place_bid.execute(cmd).await?;

    Ok(Json(BidResponse {
        bid_id: result.bid_id,
        item_id: result.item_id,
        user_id: result.user_id,
        bid_amount: result.bid_amount,
    }))
}
    
/// GET /api/items/{id}/bids/highest
/// 
/// Returns the current highest bid for the specified item.
pub async fn get_highest_bid(
    State(state): State<AppState>,
    Path(item_id): Path<String>,
) -> Result<Json<Option<HighestBidResponse>>, ApiError> {

    let cmd =  GetHighestBidCommand {item_id};

    let result = state.get_highest_bid.execute(cmd).await?;

    let response = result.map(|res| HighestBidResponse {
            user_id: res.user_id,
            bid_amount: res.bid_amount,
        });

    Ok(Json(response))
}
//! Use case: Place a bid on an item.

use std::sync::Arc;

use crate::app::dto::{PlaceBidCommand, PlaceBidResult};
use crate::app::error::AppError;
use crate::domain::bid::Bid;
use crate::domain::bid_validator::BidValidator; 
use crate::domain::types::{IdempotencyKey, ListingId, Money, UserId};
use crate::port::bid_repository::BidRepository;

pub struct PlaceBidUseCase {
    pub bid_repo: Arc<dyn BidRepository>,
}

impl PlaceBidUseCase {
    pub fn new(bid_repo: Arc<dyn BidRepository>) -> Self {
        Self { bid_repo }
    }

    pub async fn execute(&self, cmd: PlaceBidCommand) -> Result<PlaceBidResult, AppError> {
        // might add redis lock here for listing_id to prevent concurrent bid processing
        let buyer_id = UserId(cmd.buyer_id);
        let listing_id = ListingId(cmd.listing_id);
        let amount = Money(cmd.bid_amount);

        // Get current highest bid
        let current_highest_bid = self.bid_repo.get_highest_bid(&listing_id).await?;

    let current_highest_amount = current_highest_bid.map(|(_, amount)| amount);        // Validate using domain service
        BidValidator::validate_new_bid(amount, current_highest_amount)?; 

        // Create domain entity
        let idempotency_key = IdempotencyKey::new(&buyer_id, &listing_id, amount);
        let bid = Bid::new(listing_id, buyer_id, amount, idempotency_key);

        // Save bid to repository
        
        let _ = self.bid_repo.save(&bid).await?;

        // Return result
        Ok(PlaceBidResult {
            bid_id: bid.id.to_string(),
            buyer_id: bid.buyer_id.0,
            listing_id: bid.listing_id.0,
            bid_amount: bid.amount.0,
        })
    }
}
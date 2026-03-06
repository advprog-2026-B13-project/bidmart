use async_trait::async_trait;

use crate::domain::types::{ListingId, Money, UserId};
use crate::port::{BidRepository, BidRepositoryError, SaveResult};
use crate::domain::bid::Bid;

// Placeholder repository
#[derive(Clone)]
pub struct PlaceholderBidRepository;

impl PlaceholderBidRepository {
    pub fn new() -> Self {
        Self
    }
}

#[async_trait]
impl BidRepository for PlaceholderBidRepository {

    async fn save(
        &self,
        bid: &Bid,
    ) -> Result<SaveResult, BidRepositoryError> {
        println!(
            "Saving bid: listing_id={}, buyer_id={}, amount={}, idempotency_key={}, placed_at={:?}",
            bid.listing_id.0, bid.buyer_id.0, bid.amount.0, bid.idempotency_key.0, bid.placed_at
        );
        Ok(SaveResult::NewBidSaved)
    }

    async fn get_highest_bid(
        &self,
        _listing_id: &ListingId,
    ) -> Result<Option<(UserId, Money)>, BidRepositoryError> {
        Ok(None)
    }
}
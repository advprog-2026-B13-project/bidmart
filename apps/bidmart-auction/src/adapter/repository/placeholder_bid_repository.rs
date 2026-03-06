use async_trait::async_trait;

use crate::domain::types::{ItemId, Money, UserId};
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
            "Saving bid: item_id={}, user_id={}, amount={}, idempotency_key={}, placed_at={:?}",
            bid.item_id.0, bid.user_id.0, bid.amount.0, bid.idempotency_key.0, bid.placed_at
        );
        Ok(SaveResult::NewBidSaved)
    }

    async fn get_highest_bid(
        &self,
        _item_id: &ItemId,
    ) -> Result<Option<(UserId, Money)>, BidRepositoryError> {
        Ok(None)
    }
}
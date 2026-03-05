//! Port: Bid repository trait.

use async_trait::async_trait;
use thiserror::Error;

use crate::domain::bid::Bid;
use crate::domain::types::{ItemId, Money, UserId};

/// Errors from the bid repository.
#[derive(Debug, Error)]
pub enum BidRepositoryError {
    #[error("not found: {0}")]
    NotFound(String),
    #[error("unavailable: {0}")]
    Unavailable(String),
}

pub enum SaveResult {
    NewBidSaved,
    DuplicateDetected,
}

/// BidRepository port: stores and retrieves bid records.
#[async_trait]
pub trait BidRepository: Send + Sync {

    /// Save a new bid.
   async fn save(&self, bid: &Bid) -> Result<SaveResult, BidRepositoryError>;

    /// Get the highest bid for an item.
    async fn get_highest_bid(
        &self,
        item_id: &ItemId,
    ) -> Result<Option<(UserId, Money)>, BidRepositoryError>;
}

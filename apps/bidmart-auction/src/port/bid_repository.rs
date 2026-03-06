//! Port: Bid repository trait.

use async_trait::async_trait;
use thiserror::Error;

use crate::domain::bid::Bid;
use crate::domain::types::{ListingId, Money, UserId};

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

    /// Get the highest bid for an listing.
    async fn get_highest_bid(
        &self,
        listing_id: &ListingId,
    ) -> Result<Option<(UserId, Money)>, BidRepositoryError>;
}

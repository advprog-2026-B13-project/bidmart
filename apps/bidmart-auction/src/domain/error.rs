//! Domain errors: business rule violations.

use thiserror::Error;

/// Domain errors represent business rule violations.
#[derive(Debug, Error)]
pub enum DomainError {
    #[error("bid amount must be positive")]
    InvalidBidAmount,

    #[error("bid amount must be higher than current highest bid: {0}")]
    BidTooLow(i64),

    #[error("auction has ended")]
    AuctionEnded,

    #[error("auction not found: {0}")]
    AuctionNotFound(String),

    #[error("user cannot bid on their own auction")]
    CannotBidOwnAuction,

    #[error("invalid idempotency key")]
    InvalidIdempotencyKey,
}

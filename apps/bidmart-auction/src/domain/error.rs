//! Domain errors: business rule violations.

use thiserror::Error;

use crate::domain::types::Money;

/// Domain errors represent business rule violations.
#[derive(Debug, Error)]
pub enum DomainError {
    #[error("bid amount must be positive")]
    InvalidBidAmount,

    #[error("auction has not started yet")]
    AuctionNotStarted,

    #[error("auction has already ended")]
    AuctionEnded,

    #[error("bid must be at least the starting price of {0}")]
    BelowStartingPrice(i64),

    #[error("bid does not meet the minimum increment: current highest is {current}, required at least {required}")]
    MinimumIncrementNotMet { current: Money, required: Money },

    #[error("seller (id: {0}) is not allowed to bid on their own auction")]
    CannotBidOwnAuction(String),

    #[error("invalid listing: {0}")]
    InvalidListing(String),
}

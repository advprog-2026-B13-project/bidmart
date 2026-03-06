//! Domain bid entity.

use std::time::SystemTime;

use super::types::{BidId, IdempotencyKey, ListingId, Money, UserId};

/// A bid placed by a user on a listing.
#[derive(Clone, Debug)]
pub struct Bid {
    pub id: BidId,
    pub listing_id: ListingId,
    pub buyer_id: UserId,
    pub amount: Money,
    pub idempotency_key: IdempotencyKey,
    pub placed_at: SystemTime,
}

impl Bid {
    pub fn new(
        listing_id: ListingId,
        buyer_id: UserId,
        amount: Money,
        idempotency_key: IdempotencyKey,
    ) -> Self {
        Self {
            id: BidId::new(),
            listing_id,
            buyer_id,
            amount,
            idempotency_key,
            placed_at: SystemTime::now(),
        }
    }
}

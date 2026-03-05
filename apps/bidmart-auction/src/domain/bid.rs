//! Domain bid entity.

use std::time::SystemTime;

use super::types::{BidId, IdempotencyKey, ItemId, Money, UserId};

/// A bid placed by a user on an item.
#[derive(Clone, Debug)]
pub struct Bid {
    pub id: BidId,
    pub item_id: ItemId,
    pub user_id: UserId,
    pub amount: Money,
    pub idempotency_key: IdempotencyKey,
    pub placed_at: SystemTime,
}

impl Bid {
    pub fn new(
        item_id: ItemId,
        user_id: UserId,
        amount: Money,
        idempotency_key: IdempotencyKey,
    ) -> Self {
        Self {
            id: BidId::new(),
            item_id,
            user_id,
            amount,
            idempotency_key,
            placed_at: SystemTime::now(),
        }
    }
}

//! Domain listing entity.

use std::time::SystemTime;

use super::types::{ListingId, Money, UserId};

#[derive(Clone, Debug)]
pub struct Listing {
    pub id: ListingId,
    pub seller_id: UserId,
    pub starting_price: Money,
    pub start_time: SystemTime,
    pub end_time: SystemTime,
    pub minimum_increment: Money,
    pub created_at: SystemTime,
}

impl Listing {
    pub fn new(
        id: ListingId,
        seller_id: UserId,
        starting_price: Money,
        start_time: SystemTime,
        end_time: SystemTime,
        minimum_increment: Money,
    ) -> Self {
        Self {
            id,
            seller_id,
            starting_price,
            start_time,
            end_time,
            minimum_increment,
            created_at: SystemTime::now(),
        }
    }
}
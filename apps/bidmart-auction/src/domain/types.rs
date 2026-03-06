//! Domain newtypes for type-safe identifiers and values.

use serde::{Deserialize, Serialize};
use std::fmt;
use uuid::Uuid;

/// Unique listing/auction identifier (Synced from Core Service).
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct ListingId(pub Uuid);

impl fmt::Display for ListingId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

impl From<Uuid> for ListingId {
    fn from(id: Uuid) -> Self {
        Self(id)
    }
}

/// Unique user identifier (Synced from Core Service/JWT).
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct UserId(pub Uuid);

impl fmt::Display for UserId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

impl From<Uuid> for UserId {
    fn from(id: Uuid) -> Self {
        Self(id)
    }
}

/// Unique bid identifier (Generated locally as UUID v4).
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct BidId(pub Uuid);

impl BidId {
    pub fn new() -> Self {
        Self(Uuid::new_v4())
    }
}

impl Default for BidId {
    fn default() -> Self {
        Self::new()
    }
}

impl fmt::Display for BidId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

/// Monetary amount (avoids floating-point issues).
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, PartialOrd, Ord, Serialize, Deserialize)]
pub struct Money(pub i64);

impl Money {
    #[allow(dead_code)]
    pub fn from_rupiah(rupiah: i64) -> Self {
        Money(rupiah)
    }

    #[allow(dead_code)]
    pub fn rupiah(&self) -> i64 {
        self.0
    }
}

impl fmt::Display for Money {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

/// Idempotency key for preventing duplicate bids.
#[derive(Clone, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct IdempotencyKey(pub String);

impl IdempotencyKey {
    /// Create idempotency key from buyer_id, listing_id, and bid_amount.
    pub fn new(buyer_id: &UserId, listing_id: &ListingId, amount: Money) -> Self {
        Self(format!("bid:{}:{}:{}", listing_id.0, buyer_id.0, amount.0))
    }
}

impl fmt::Display for IdempotencyKey {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

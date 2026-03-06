//! Domain newtypes for type-safe identifiers and values.

use std::fmt;
use uuid::Uuid;

/// Unique item/auction identifier.
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub struct ListingId(pub String);

impl ListingId {
    pub fn new(id: impl Into<String>) -> Self {
        Self(id.into())
    }
}

impl fmt::Display for ListingId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

impl From<String> for ListingId {
    fn from(s: String) -> Self {
        ListingId(s)
    }
}

/// Unique user identifier (from JWT).
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub struct UserId(pub String);

impl UserId {
    pub fn new(id: impl Into<String>) -> Self {
        Self(id.into())
    }
}

impl fmt::Display for UserId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

impl From<String> for UserId {
    fn from(s: String) -> Self {
        UserId(s)
    }
}

/// Unique bid identifier (UUID v4).
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub struct BidId(pub Uuid);

impl BidId {
    pub fn new() -> Self {
        Self(Uuid::new_v4())
    }
}

impl fmt::Display for BidId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

/// Monetary amount (avoids floating-point issues).
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct Money(pub i64);

impl Money {
    pub fn from_rupiah(rupiah: i64) -> Self {
        Money(rupiah)
    }

    pub fn rupiah(&self) -> i64 {
        self.0
    }


}

impl fmt::Display for Money {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

impl From<i64> for Money {
    fn from(rupiah: i64) -> Self {
        Money(rupiah)
    }
}

/// Idempotency key for preventing duplicate bids.
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub struct IdempotencyKey(pub String);

impl IdempotencyKey {
    /// Create idempotency key from buyer_id, listing_id, and bid_amount.
    pub fn new(buyer_id: &UserId, listing_id: &ListingId, amount: Money) -> Self {
        Self(format!("bid:{}:{}:{}", listing_id, buyer_id, amount))
    }
}

impl fmt::Display for IdempotencyKey {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

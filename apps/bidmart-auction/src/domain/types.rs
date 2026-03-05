//! Domain newtypes for type-safe identifiers and values.

use std::fmt;
use uuid::Uuid;

/// Unique item/auction identifier.
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub struct ItemId(pub String);

impl ItemId {
    pub fn new(id: impl Into<String>) -> Self {
        Self(id.into())
    }
}

impl fmt::Display for ItemId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

impl From<String> for ItemId {
    fn from(s: String) -> Self {
        ItemId(s)
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
// may switch to rupiah only
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct Money(pub i64);

impl Money {
    pub fn from_cents(cents: i64) -> Self {
        Self(cents)
    }

    pub fn from_dollars(dollars: f64) -> Self {
        Self((dollars * 100.0).round() as i64)
    }

    pub fn cents(&self) -> i64 {
        self.0
    }
}

impl fmt::Display for Money {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

impl From<i64> for Money {
    fn from(cents: i64) -> Self {
        Money(cents)
    }
}

/// Idempotency key for preventing duplicate bids.
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub struct IdempotencyKey(pub String);

impl IdempotencyKey {
    /// Create idempotency key from user_id, item_id, and bid_amount.
    pub fn new(user_id: &UserId, item_id: &ItemId, amount: Money) -> Self {
        Self(format!("bid:{}:{}:{}", item_id, user_id, amount))
    }
}

impl fmt::Display for IdempotencyKey {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

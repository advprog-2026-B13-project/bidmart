use async_trait::async_trait;
use sqlx::postgres::PgPool;

use crate::domain::bid::Bid;
use crate::domain::types::{ListingId, Money, UserId};
use crate::port::bid_repository::{BidRepositoryError, SaveResult};
use crate::port::BidRepository;

/// PostgreSQL bid repository.
#[derive(Clone)]
pub struct PostgresBidRepository {
    pool: PgPool,
}

impl PostgresBidRepository {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl BidRepository for PostgresBidRepository {
    async fn save(&self, bid: &Bid) -> Result<SaveResult, BidRepositoryError> {
        tracing::info!(
            "Saving bid: id={}, listing_id={}, amount={}, idempotency_key={}",
            bid.id.0,
            bid.listing_id.0,
            bid.amount.0,
            bid.idempotency_key.0
        );

        let result = sqlx::query!(
            r#"
            INSERT INTO bids (id, listing_id, buyer_id, amount, idempotency_key, placed_at)
            VALUES ($1, $2, $3, $4, $5, $6)
            ON CONFLICT (idempotency_key) DO NOTHING
            "#,
            bid.id.0,
            bid.listing_id.0,
            bid.buyer_id.0,
            bid.amount.0,
            bid.idempotency_key.0,
            chrono::DateTime::<chrono::Utc>::from(bid.placed_at)
        )
        .execute(&self.pool)
        .await
        .map_err(|e| BidRepositoryError::Unavailable(e.to_string()))?;

        if result.rows_affected() == 0 {
            Ok(SaveResult::DuplicateDetected)
        } else {
            Ok(SaveResult::NewBidSaved)
        }
    }

    async fn get_highest_bid(
        &self,
        listing_id: &ListingId,
    ) -> Result<Option<(UserId, Money)>, BidRepositoryError> {
        let row = sqlx::query!(
            r#"
            SELECT buyer_id, amount FROM bids
            WHERE listing_id = $1
            ORDER BY amount DESC
            LIMIT 1
            "#,
            listing_id.0
        )
        .fetch_optional(&self.pool)
        .await
        .map_err(|e| BidRepositoryError::Unavailable(e.to_string()))?;

        match row {
            Some(r) => Ok(Some((UserId(r.buyer_id), Money(r.amount)))),
            None => Ok(None),
        }
    }
}

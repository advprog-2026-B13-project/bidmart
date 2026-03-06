use async_trait::async_trait;
use sqlx::postgres::PgPool;

use crate::domain::bid::Bid;
use crate::domain::types::{ItemId, Money, UserId};
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
            "Saving bid: id={}, item_id={}, amount={}, idempotency_key={}",
            bid.id.0, bid.item_id.0, bid.amount.0, bid.idempotency_key.0
        );

        let result = sqlx::query!(
            r#"
            INSERT INTO bids (id, item_id, user_id, amount, idempotency_key, placed_at)
            VALUES ($1, $2, $3, $4, $5, $6)
            ON CONFLICT (idempotency_key) DO NOTHING
            "#,
            bid.id.0,
            bid.item_id.0,
            bid.user_id.0,
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
        item_id: &ItemId,
    ) -> Result<Option<(UserId, Money)>, BidRepositoryError> {
        let row = sqlx::query!(
            r#"
            SELECT user_id, amount FROM bids
            WHERE item_id = $1
            ORDER BY amount DESC
            LIMIT 1
            "#,
            item_id.0
        )
        .fetch_optional(&self.pool)
        .await
        .map_err(|e| BidRepositoryError::Unavailable(e.to_string()))?;

        match row {
            Some(r) => Ok(Some((UserId(r.user_id), Money(r.amount)))),
            None => Ok(None),
        }
    }
}
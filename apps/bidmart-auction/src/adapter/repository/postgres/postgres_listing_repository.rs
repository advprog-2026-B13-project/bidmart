//! Implementation: PostgreSQL listing repository.

use async_trait::async_trait;
use sqlx::postgres::PgPool;
use std::time::SystemTime;

use crate::domain::listing::Listing;
use crate::domain::types::{ListingId, Money, UserId};
use crate::port::listing_repository::{ListingRepository, ListingRepositoryError};

/// PostgreSQL listing repository.
#[derive(Clone)]
pub struct PostgresListingRepository {
    pool: PgPool,
}

impl PostgresListingRepository {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

#[async_trait]
impl ListingRepository for PostgresListingRepository {
    async fn save(&self, listing: &Listing) -> Result<(), ListingRepositoryError> {
        tracing::info!(
            "Saving listing: id={}, seller_id={}",
            listing.id.0,
            listing.seller_id.0
        );

        sqlx::query!(
            r#"
            INSERT INTO listings (
                id, seller_id, starting_price, start_time, 
                end_time, minimum_increment, created_at
            )
            VALUES ($1, $2, $3, $4, $5, $6, $7)
            "#,
            listing.id.0,
            listing.seller_id.0,
            listing.starting_price.0,
            chrono::DateTime::<chrono::Utc>::from(listing.start_time),
            chrono::DateTime::<chrono::Utc>::from(listing.end_time),
            listing.minimum_increment.0,
            chrono::DateTime::<chrono::Utc>::from(listing.created_at),
        )
        .execute(&self.pool)
        .await
        .map_err(|e| {
            if let Some(db_err) = e.as_database_error() {
                if db_err.code() == Some("23505".into()) {
                    // "230505" is the SQLSTATE code for unique violation in PostgreSQL
                    return ListingRepositoryError::AlreadyExists(listing.id);
                }
            }

            ListingRepositoryError::InfrastructureError(e.to_string())
        })?;

        Ok(())
    }

    async fn find_by_id(&self, id: &ListingId) -> Result<Option<Listing>, ListingRepositoryError> {
        let row = sqlx::query!(
            r#"
            SELECT 
                id as "id!", 
                seller_id, 
                starting_price, 
                start_time as "start_time!", 
                end_time as "end_time!", 
                minimum_increment, 
                created_at as "created_at!"        
            FROM listings
            WHERE id = $1
            "#,
            id.0
        )
        .fetch_optional(&self.pool)
        .await
        .map_err(|e| ListingRepositoryError::InfrastructureError(e.to_string()))?;

        // Now row.start_time is DateTime<Utc>, not Option<DateTime<Utc>>
        match row {
            Some(r) => Ok(Some(Listing {
                id: ListingId(r.id),
                seller_id: UserId(r.seller_id),
                starting_price: Money(r.starting_price),
                start_time: SystemTime::from(r.start_time),
                end_time: SystemTime::from(r.end_time),
                minimum_increment: Money(r.minimum_increment),
                created_at: SystemTime::from(r.created_at),
            })),
            None => Err(ListingRepositoryError::NotFound(*id)),
        }
    }

    async fn update(&self, listing: &Listing) -> Result<(), ListingRepositoryError> {
        tracing::info!("Updating listing: id={}", listing.id.0);

        let result = sqlx::query!(
            r#"
            UPDATE listings
            SET 
                seller_id = $2,
                starting_price = $3,
                start_time = $4,
                end_time = $5,
                minimum_increment = $6
            WHERE id = $1
            "#,
            listing.id.0,
            listing.seller_id.0,
            listing.starting_price.0,
            chrono::DateTime::<chrono::Utc>::from(listing.start_time),
            chrono::DateTime::<chrono::Utc>::from(listing.end_time),
            listing.minimum_increment.0,
        )
        .execute(&self.pool)
        .await
        .map_err(|e| ListingRepositoryError::InfrastructureError(e.to_string()))?;

        // If no rows were affected, the listing likely doesn't exist.
        if result.rows_affected() == 0 {
            return Err(ListingRepositoryError::NotFound(listing.id));
        }

        Ok(())
    }

    async fn delete(&self, id: &ListingId) -> Result<(), ListingRepositoryError> {
        tracing::info!("Deleting listing: id={}", id.0);

        let result = sqlx::query!(
            r#"
            DELETE FROM listings
            WHERE id = $1
            "#,
            id.0
        )
        .execute(&self.pool)
        .await
        .map_err(|e| ListingRepositoryError::InfrastructureError(e.to_string()))?;

        if result.rows_affected() == 0 {
            return Err(ListingRepositoryError::NotFound(*id));
        }

        Ok(())
    }
}

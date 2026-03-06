//! Port: Listing repository trait.

use async_trait::async_trait;
use thiserror::Error;
use std::time::SystemTime;

use crate::domain::listing::Listing;
use crate::domain::types::{ListingId};

/// Errors from the listing repository.
#[derive(Debug, Error)]
pub enum ListingRepositoryError {
    #[error("listing not found: {0}")]
    NotFound(ListingId),
    #[error("database error: {0}")]
    InfrastructureError(String),
    #[error("concurrency error: {0}")]
    Conflict(String),
}

/// ListingRepository port: manages the lifecycle of auction items.
#[async_trait]
pub trait ListingRepository: Send + Sync {
    
    /// Persist a new listing to the store.
    async fn save(&self, listing: &Listing) -> Result<(), ListingRepositoryError>;

    async fn find_by_id(&self, id: &ListingId) -> Result<Option<Listing>, ListingRepositoryError>;

    async fn update(&self, listing: &Listing) -> Result<(), ListingRepositoryError>;

    async fn is_active(&self, id: &ListingId, at: SystemTime) -> Result<bool, ListingRepositoryError>;
}
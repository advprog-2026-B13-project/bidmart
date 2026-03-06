//! Application-layer error type.

use thiserror::Error;

use crate::domain::error::DomainError;
use crate::port::{BidRepositoryError, ListingRepositoryError};

/// Unified application error mapped to HTTP status codes.
#[derive(Debug, Error)]
pub enum AppError {
    #[error("validation error: {0}")]
    Validation(#[from] DomainError),

    #[error("not found: {0}")]
    NotFound(String),

    #[allow(dead_code)]
    #[error("unauthorized: {0}")]
    Unauthorized(String),

    #[error("internal error: {0}")]
    Internal(String),
}

impl From<BidRepositoryError> for AppError {
    fn from(e: BidRepositoryError) -> Self {
        match e {
            BidRepositoryError::Unavailable(msg) => AppError::Internal(msg),
        }
    }
}

impl From<ListingRepositoryError> for AppError {
    fn from(e: ListingRepositoryError) -> Self {
        match e {
            ListingRepositoryError::NotFound(id) => {
                AppError::NotFound(format!("Listing with ID {} not found", id.0))
            }
            ListingRepositoryError::InfrastructureError(msg) => AppError::Internal(msg),
            ListingRepositoryError::AlreadyExists(id) => AppError::Validation(
                DomainError::InvalidListing(format!("Listing with ID {} already exists", id.0)),
            ),
        }
    }
}

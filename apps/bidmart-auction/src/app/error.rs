//! Application-layer error type.

use thiserror::Error;

use crate::domain::error::DomainError;
use crate::port::bid_repository::BidRepositoryError;

/// Unified application error mapped to HTTP status codes.
#[derive(Debug, Error)]
pub enum AppError {
    #[error("validation error: {0}")]
    Validation(#[from] DomainError),

    #[error("not found: {0}")]
    NotFound(String),

    #[error("unauthorized: {0}")]
    Unauthorized(String),

    #[error("internal error: {0}")]
    Internal(String),
}

impl From<BidRepositoryError> for AppError {
    fn from(e: BidRepositoryError) -> Self {
        match e {
            BidRepositoryError::NotFound(msg) => AppError::NotFound(msg),
            BidRepositoryError::Unavailable(msg) => AppError::Internal(msg),
        }
    }
}

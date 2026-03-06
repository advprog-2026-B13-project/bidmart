//! Use case: Register a new auction listing.

use std::sync::Arc;
use std::time::SystemTime;

use crate::app::dto::{RegisterListingCommand, RegisterListingResult};
use crate::app::error::AppError;
use crate::domain::error::DomainError;
use crate::domain::listing::Listing;
use crate::domain::types::{ListingId, Money, UserId};
use crate::port::listing_repository::ListingRepository;

pub struct RegisterListingUseCase {
    pub listing_repo: Arc<dyn ListingRepository>,
}

impl RegisterListingUseCase {
    pub fn new(listing_repo: Arc<dyn ListingRepository>) -> Self {
        Self { listing_repo }
    }

    pub async fn execute(&self, cmd: RegisterListingCommand) -> Result<RegisterListingResult, AppError> {
        let listing_id = ListingId(cmd.id);
        let seller_id = UserId(cmd.seller_id);
        let starting_price = Money(cmd.starting_price);
        let min_increment = Money(cmd.minimum_increment);
        
        let start_time = SystemTime::from(cmd.start_time);
        let end_time = SystemTime::from(cmd.end_time);

        if end_time <= start_time {
            return Err(AppError::Validation(DomainError::InvalidListing("end_time must be after start_time".to_string())));
        }
        
        if starting_price.0 < 0 {
            return Err(AppError::Validation(DomainError::InvalidListing("starting_price cannot be negative".to_string())));
        }

        let listing = Listing::new(
            listing_id,
            seller_id,
            starting_price,
            start_time,
            end_time,
            min_increment,
        );

        self.listing_repo.save(&listing).await.map_err(|e| {
            AppError::Internal(e.to_string())
        })?;

        Ok(RegisterListingResult {
            listing_id: listing.id.0,
        })
    }
}
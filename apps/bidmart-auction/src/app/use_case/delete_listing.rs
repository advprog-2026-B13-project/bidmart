//! Use case: Delete an auction listing.

use std::sync::Arc;

use crate::app::dto::{DeleteListingCommand, DeleteListingResult};
use crate::app::error::AppError;
use crate::domain::types::ListingId;
use crate::port::listing_repository::ListingRepository;

pub struct DeleteListingUseCase {
    pub listing_repo: Arc<dyn ListingRepository>,
}

impl DeleteListingUseCase {
    pub fn new(listing_repo: Arc<dyn ListingRepository>) -> Self {
        Self { listing_repo }
    }

    pub async fn execute(
        &self,
        cmd: DeleteListingCommand,
    ) -> Result<DeleteListingResult, AppError> {
        let listing_id = ListingId(cmd.listing_id);

        tracing::info!("Deleting listing: id={}", listing_id.0);

        self.listing_repo.delete(&listing_id).await?;

        Ok(DeleteListingResult {
            listing_id: cmd.listing_id,
        })
    }
}

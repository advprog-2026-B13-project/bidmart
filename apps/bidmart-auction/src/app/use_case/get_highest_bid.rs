use std::sync::Arc;

use crate::app::dto::{GetHighestBidCommand, GetHighestBidResult};
use crate::app::error::AppError;
use crate::domain::types::ListingId;
use crate::port::bid_repository::BidRepository;

pub struct GetHighestBidUseCase {
    pub bid_repo: Arc<dyn BidRepository>,
}

impl GetHighestBidUseCase {
    pub fn new(bid_repo: Arc<dyn BidRepository>) -> Self {
        Self { bid_repo }
    }

    pub async fn execute(
        &self,
        cmd: GetHighestBidCommand,
    ) -> Result<Option<GetHighestBidResult>, AppError> {
        let listing_id = ListingId(cmd.listing_id);
        let highest_bid = self.bid_repo.get_highest_bid(&listing_id).await?;

        Ok(highest_bid.map(|(buyer_id, amount)| GetHighestBidResult {
            buyer_id: buyer_id.0,
            bid_amount: amount.0,
        }))
    }
}

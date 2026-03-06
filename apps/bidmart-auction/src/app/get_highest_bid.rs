use std::sync::Arc;

use crate::app::error::AppError;
use crate::domain::types::{ItemId};
use crate::app::dto::{GetHighestBidCommand, GetHighestBidResult};
use crate::port::bid_repository::BidRepository;

pub struct GetHighestBidUseCase {
    pub bid_repo: Arc<dyn BidRepository>,
}

impl GetHighestBidUseCase {
    pub fn new(bid_repo: Arc<dyn BidRepository>) -> Self {
        Self { bid_repo }
    }

    pub async fn execute(&self, cmd: GetHighestBidCommand) -> Result<Option<GetHighestBidResult>, AppError> {
        let item_id = ItemId(cmd.item_id);
        let highest_bid = self.bid_repo.get_highest_bid(&item_id).await?;
        
        Ok(highest_bid.map(|(user_id, amount)| GetHighestBidResult {
            user_id: user_id.0,
            bid_amount: amount.0,
        }))
    }
}
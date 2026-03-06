//! Use case: Place a bid on a listing.

use std::sync::Arc;
use std::time::SystemTime;

use crate::app::dto::{PlaceBidCommand, PlaceBidResult};
use crate::app::error::AppError;
use crate::domain::bid::Bid;
use crate::domain::bid_validator::BidValidator; 
use crate::domain::error::DomainError;
use crate::domain::types::{IdempotencyKey, ListingId, Money, UserId};
use crate::port::bid_repository::BidRepository;
// Import port baru
use crate::port::listing_repository::ListingRepository;

pub struct PlaceBidUseCase {
    pub bid_repo: Arc<dyn BidRepository>,
    pub listing_repo: Arc<dyn ListingRepository>, 
}

impl PlaceBidUseCase {
    pub fn new(
        bid_repo: Arc<dyn BidRepository>, 
        listing_repo: Arc<dyn ListingRepository> 
    ) -> Self {
        Self { bid_repo, listing_repo }
    }

    pub async fn execute(&self, cmd: PlaceBidCommand) -> Result<PlaceBidResult, AppError> {
        let buyer_id = UserId(cmd.buyer_id);
        let listing_id = ListingId(cmd.listing_id);
        let amount = Money(cmd.bid_amount);

        let listing = self.listing_repo.find_by_id(&listing_id).await?
            .ok_or_else(|| AppError::NotFound(format!("Listing {} not found", listing_id.0)))?;

        let now = SystemTime::now();
        if now < listing.start_time || now > listing.end_time {
            return Err(AppError::Validation(DomainError::AuctionNotStarted));
        }

        // 3. Ambil bid tertinggi saat ini
        let current_highest_bid = self.bid_repo.get_highest_bid(&listing_id).await?;
        let current_highest_amount = current_highest_bid.map(|(_, amount)| amount);

        // 4. Panggil validator dengan parameter lengkap
        BidValidator::validate_new_bid(
            amount, 
            current_highest_amount,
            listing.starting_price,
            listing.minimum_increment
        )?; 

        // 5. Buat domain entity
        let idempotency_key = IdempotencyKey::new(&buyer_id, &listing_id, amount);
        let bid = Bid::new(listing_id, buyer_id, amount, idempotency_key);

        // 6. Simpan bid
        let _ = self.bid_repo.save(&bid).await?;

        // Return result
        Ok(PlaceBidResult {
            bid_id: bid.id.0,
            buyer_id: bid.buyer_id.0,
            listing_id: bid.listing_id.0,
            bid_amount: bid.amount.0,
        })
    }
}
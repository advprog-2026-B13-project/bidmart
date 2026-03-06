
use crate::domain::error::DomainError;
use crate::domain::types::Money;

pub struct BidValidator;

impl BidValidator {
    /// Validate a new bid against business rules.
    pub fn validate_new_bid(
        amount: Money,
        current_highest: Option<Money>,
    ) -> Result<(), DomainError> {
        if amount.rupiah() <= 0 {
            return Err(DomainError::InvalidBidAmount);
        }


        // Validate bid is higher than current highest
        // todo: english auction increment
        if let Some(highest) = current_highest {
            if amount <= highest {
                return Err(DomainError::BidTooLow(highest.rupiah()));
            }
        }

        Ok(())
    }
}
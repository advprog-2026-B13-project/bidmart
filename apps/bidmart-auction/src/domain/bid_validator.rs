use crate::domain::error::DomainError;
use crate::domain::types::Money;

pub struct BidValidator;

impl BidValidator {
    /// Validate a new bid against business rules.
    pub fn validate_new_bid(
        amount: Money,
        current_highest: Option<Money>,
        starting_price: Money,
        minimum_increment: Money,
    ) -> Result<(), DomainError> {
        if amount.0 <= 0 {
            return Err(DomainError::InvalidBidAmount);
        }

        match current_highest {
            Some(highest) => {
                let required_minimum = Money(highest.0 + minimum_increment.0);
                if amount < required_minimum {
                    return Err(DomainError::MinimumIncrementNotMet {
                        current: highest,
                        required: required_minimum,
                    });
                }
            }
            None => {
                if amount < starting_price {
                    return Err(DomainError::BelowStartingPrice(starting_price.0));
                }
            }
        }

        Ok(())
    }
}

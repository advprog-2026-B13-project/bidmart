package id.ac.ui.cs.advprog.bidmartcore.bidding.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionValidatorTest {

    private AuctionValidator validator;
    private UUID sellerId;
    private UUID bidderId;

    @BeforeEach
    void setUp() {
        validator = new AuctionValidator();
        sellerId = UUID.randomUUID();
        bidderId = UUID.randomUUID();
    }

    @Test
    void validateStatic_validBid_doesNotThrow() {
        assertDoesNotThrow(() ->
                validator.validateStatic(sellerId, BigDecimal.valueOf(1000), bidderId));
    }

    @Test
    void validateStatic_sellerBidsOwnListing_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                validator.validateStatic(sellerId, BigDecimal.valueOf(1000), sellerId));
    }

    @Test
    void validateStatic_zeroBidAmount_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                validator.validateStatic(sellerId, BigDecimal.ZERO, bidderId));
    }

    @Test
    void validateStatic_negativeBidAmount_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                validator.validateStatic(sellerId, BigDecimal.valueOf(-1), bidderId));
    }

    @Test
    void validateStatic_nullBidAmount_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                validator.validateStatic(sellerId, null, bidderId));
    }
}

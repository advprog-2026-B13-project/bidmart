package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AuctionClosedEvent {
    private final UUID listingId;
    private final UUID sellerId;
    private final UUID winnerBidderId;     // null if unsold
    private final BigDecimal finalAmount; // null if unsold
    private final AuctionResult result;
    private final LocalDateTime timestamp;

    public enum AuctionResult { WON, UNSOLD }
}

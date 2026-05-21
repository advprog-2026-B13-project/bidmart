package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OutbidEvent {
    private final UUID listingId;
    private final UUID outbidBidderId;
    private final BigDecimal previousBidAmount;
    private final BigDecimal newAmount;
    private final LocalDateTime timestamp;
    private final BigDecimal heldAmount;
}

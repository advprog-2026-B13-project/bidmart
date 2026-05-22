package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class BidPlacedEvent {
    private final UUID listingId;
    private final UUID bidId;
    private final UUID bidderId;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;
}

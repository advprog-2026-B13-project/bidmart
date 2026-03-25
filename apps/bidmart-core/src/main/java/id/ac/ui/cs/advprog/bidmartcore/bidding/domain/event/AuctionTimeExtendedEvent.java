package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AuctionTimeExtendedEvent {
    private final UUID listingId;
    private final LocalDateTime previousEndTime;
    private final LocalDateTime newEndTime;
}

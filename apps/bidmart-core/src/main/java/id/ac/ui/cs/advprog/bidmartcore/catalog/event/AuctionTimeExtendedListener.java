package id.ac.ui.cs.advprog.bidmartcore.catalog.event;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionTimeExtendedEvent;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.ListingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionTimeExtendedListener {

    private final ListingService listingService;

    @EventListener
    public void onAuctionTimeExtended(AuctionTimeExtendedEvent event) {
        log.info("Auction {} extended: {} -> {}",
                event.getListingId(), event.getPreviousEndTime(), event.getNewEndTime());
        listingService.updateEndTime(event.getListingId(), event.getNewEndTime());
    }
}

package id.ac.ui.cs.advprog.bidmartcore.catalog.event;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.BidPlacedEvent;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.ListingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidPlacedListener {

    private final ListingService listingService;

    @EventListener
    public void onBidPlaced(BidPlacedEvent event) {
        log.debug("Bid placed on auction {}: {} bid {}",
                event.getListingId(), event.getBidderId(), event.getAmount());
        listingService.updateCurrentPriceAndWinner(
                event.getListingId(), event.getAmount(), event.getBidderId());
    }
}

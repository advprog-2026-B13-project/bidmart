package id.ac.ui.cs.advprog.bidmartcore.catalog.event;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionClosedEvent;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionClosedEvent.AuctionResult;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.ListingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component("catalogAuctionTimeExtendedListener")
@RequiredArgsConstructor
public class AuctionClosedListener {

    private final ListingService listingService;

    @EventListener
    public void onAuctionClosed(AuctionClosedEvent event) {
        log.info("Auction {} closed with result: {}", event.getListingId(), event.getResult());

        if (event.getResult() == AuctionResult.WON) {
            listingService.updateStatus(event.getListingId(), ListingStatus.WON);
            listingService.updateCurrentPriceAndWinner(
                    event.getListingId(), event.getFinalAmount(), event.getWinnerBidderId());
        } else {
            listingService.updateStatus(event.getListingId(), ListingStatus.UNSOLD);
        }
    }
}

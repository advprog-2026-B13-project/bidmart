package id.ac.ui.cs.advprog.bidmartcore.catalog.event;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.BidPlacedEvent;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.ListingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component("catalogBidPlacedListener")
@RequiredArgsConstructor
public class BidPlacedListener {

    private final ListingService listingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBidPlaced(BidPlacedEvent event) {
        log.debug("Bid placed on auction {}: {} bid {}",
                event.getListingId(), event.getBidderId(), event.getAmount());
        listingService.updateCurrentPriceAndWinner(
                event.getListingId(), event.getAmount(), event.getBidderId());
    }
}

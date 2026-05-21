package id.ac.ui.cs.advprog.bidmartcore.notification.event;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.BidPlacedEvent;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.ListingRepository;
import id.ac.ui.cs.advprog.bidmartcore.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidPlacedNotificationListener {

    private final NotificationService notificationService;
    private final ListingRepository listingRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBidPlaced(BidPlacedEvent event) {
        log.debug("Bid placed event for listing {}, bidder {}: amount {}",
                event.getListingId(), event.getBidderId(), event.getAmount());

        // Notify seller only — bidder gets a toast confirmation on the frontend
        listingRepository.findById(event.getListingId()).ifPresent(listing -> {
            String sellerMessage = String.format(
                    "New bid of Rp %s on your listing \"%s\"!",
                    formatAmount(event.getAmount()),
                    truncate(listing.getTitle(), 50)
            );
            notificationService.createNotification(
                    listing.getSellerId(),
                    "NEW_BID",
                    sellerMessage
            );
        });
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ".");
    }

    private String truncate(String s, int maxLen) {
        return s == null ? "" : (s.length() > maxLen ? s.substring(0, maxLen) + "..." : s);
    }
}
package id.ac.ui.cs.advprog.bidmartcore.notification.event;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.OutbidEvent;
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
public class OutbidListener {

    private final NotificationService notificationService;
    private final ListingRepository listingRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOutbid(OutbidEvent event) {
        log.debug("Outbid event for bidder {} on listing {}: was {} now {}",
                event.getOutbidBidderId(), event.getListingId(),
                event.getPreviousBidAmount(), event.getNewAmount());

        String listingTitle = listingRepository.findById(event.getListingId())
                .map(Listing::getTitle)
                .orElse("Item");

        String message = String.format(
                "You've been outbid on '%s'! New highest bid is Rp %s. Bid again to stay in the lead.",
                listingTitle,
                formatAmount(event.getNewAmount())
        );

        notificationService.createNotification(
                event.getOutbidBidderId(),
                "OUTBID",
                message,
                event.getListingId()
        );
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toString().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ".");
    }
}
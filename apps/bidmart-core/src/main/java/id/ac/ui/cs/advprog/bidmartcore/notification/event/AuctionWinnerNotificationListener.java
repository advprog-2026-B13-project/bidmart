package id.ac.ui.cs.advprog.bidmartcore.notification.event;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionClosedEvent;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.ListingRepository;
import id.ac.ui.cs.advprog.bidmartcore.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionWinnerNotificationListener {

    private final NotificationService notificationService;
    private final ListingRepository listingRepository;

    @EventListener
    public void onAuctionClosed(AuctionClosedEvent event) {
        if (event.getResult() == AuctionClosedEvent.AuctionResult.WON) {
            log.debug("Auction WON event for listing {}, winner {}: final amount {}",
                    event.getListingId(), event.getWinnerBidderId(), event.getFinalAmount());

            listingRepository.findById(event.getListingId()).ifPresent(listing -> {

                String message = String.format(
                        "Congratulations! You won the auction for \"%s\" with a final bid of Rp %s. " +
                                "Your order has been automatically created. Please complete your payment soon.",
                        truncate(listing.getTitle(), 50),
                        formatAmount(event.getFinalAmount())
                );

                notificationService.createNotification(
                        event.getWinnerBidderId(),
                        "AUCTION_WON",
                        message,
                        listing.getId()
                );
            });
        }
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0";
        return amount.toString().replaceAll("\\B(?=(\\d{3})+(?!\\d))", ".");
    }

    private String truncate(String s, int maxLen) {
        return s == null ? "" : (s.length() > maxLen ? s.substring(0, maxLen) + "..." : s);
    }
}
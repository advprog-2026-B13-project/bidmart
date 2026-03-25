package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.redis;

import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ConcurrencyPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort.ListingInfo;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionCacheWarmupService {

    private final ListingRepository listingRepository;
    private final ConcurrencyPort concurrencyPort;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        List<id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing> activeListings =
                listingRepository.findByStatus(ListingStatus.ACTIVE);

        if (activeListings.isEmpty()) {
            log.info("No active listings found for cache warmup");
            return;
        }

        log.info("Warming up auction cache with {} active listings", activeListings.size());

        for (var listing : activeListings) {
            ListingInfo info = new ListingInfo(
                    listing.getSellerId(),
                    listing.getStatus(),
                    listing.getStartingPrice(),
                    listing.getCurrentPrice(),
                    listing.getReservePrice(),
                    listing.getMinBidIncrement(),
                    listing.getEndTime(),
                    listing.getWinnerId()
            );
            concurrencyPort.refreshAuction(listing.getId(), info);
            log.debug("Cached auction state for listing {}", listing.getId());
        }

        log.info("Auction cache warmup complete");
    }
}

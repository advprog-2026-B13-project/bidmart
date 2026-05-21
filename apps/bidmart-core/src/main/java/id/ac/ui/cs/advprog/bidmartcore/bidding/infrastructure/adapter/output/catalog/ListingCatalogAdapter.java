package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.catalog;

import java.util.UUID;

import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ConcurrencyPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.ListingService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ListingCatalogAdapter implements ListingPort {

    private final ListingService listingService;
    private final ConcurrencyPort concurrencyPort;

    @Override
    public ListingInfo getListingInfo(UUID listingId) {
        ListingInfo cached = concurrencyPort.getListingInfoFromCache(listingId);
        if (cached != null && cached.status() == ListingStatus.ACTIVE) {
            return cached;
        }

        Listing listing = listingService.getListingById(listingId);
        ListingInfo info = new ListingInfo(
                listing.getSellerId(),
                listing.getStatus(),
                listing.getStartingPrice(),
                listing.getCurrentPrice(),
                listing.getReservePrice(),
                listing.getMinBidIncrement(),
                listing.getStartTime(),
                listing.getEndTime(),
                listing.getWinnerId()
        );

        if (listing.getStatus() == ListingStatus.ACTIVE) {
            concurrencyPort.cacheAuction(listingId, info);
        }
        return info;
    }
}

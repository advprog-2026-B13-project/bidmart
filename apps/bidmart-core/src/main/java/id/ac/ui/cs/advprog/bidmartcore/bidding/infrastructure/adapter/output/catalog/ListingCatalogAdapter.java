package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.catalog;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort.ListingInfo;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ListingCatalogAdapter implements ListingPort {

    private final ListingService listingService;

    @Override
    public ListingInfo getListingInfo(UUID listingId) {
        Listing listing = listingService.getListingById(listingId);
        return new ListingInfo(
                listing.getSellerId(),
                listing.getStatus(),
                listing.getStartingPrice(),
                listing.getCurrentPrice(),
                listing.getEndTime()
        );
    }

    @Override
    public void updateCurrentPriceAndWinner(UUID listingId, BigDecimal newPrice, UUID winnerId) {
        listingService.updateCurrentPriceAndWinner(listingId, newPrice, winnerId);
    }

    @Override
    public void updateStatus(UUID listingId, ListingStatus status) {
        listingService.updateStatus(listingId, status);
    }
}

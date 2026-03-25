package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.ConcurrencyResult;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort.ListingInfo;

import java.util.UUID;

public interface ConcurrencyPort {

    ConcurrencyResult placeBid(UUID listingId, long bidAmount, long minIncrement,
                               UUID bidderId, long currentTimeMillis,
                               long antiSnipeThresholdMillis, long antiSnipeExtensionMillis);

    void rollback(UUID listingId, long priceToRestore, String winnerToRestore, long endTimeToRestore);

    void cacheAuction(UUID listingId, ListingInfo info);

    void refreshAuction(UUID listingId, ListingInfo info);

    void removeAuction(UUID listingId);

    long getAuctionEndTime(UUID listingId);
}

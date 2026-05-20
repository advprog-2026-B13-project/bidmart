package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output;

import java.util.UUID;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidType;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.ConcurrencyResult;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort.ListingInfo;

public interface ConcurrencyPort {

    ConcurrencyResult placeBid(UUID listingId, long bidAmount, long minIncrement,
            UUID bidderId, BidType bidType, long currentTimeMillis,
                               long antiSnipeThresholdMillis, long antiSnipeExtensionMillis);

    void rollback(UUID listingId,
                  long priceToRestore,
                  String winnerToRestore,
                  long endTimeToRestore,
                  long expectedCurrentPrice,
                  String expectedCurrentWinner);

    void cacheAuction(UUID listingId, ListingInfo info);

    void refreshAuction(UUID listingId, ListingInfo info);

    ListingPort.ListingInfo getListingInfoFromCache(UUID listingId);

    void removeAuction(UUID listingId);

    long getAuctionEndTime(UUID listingId);

    record LiveAuctionState(long priceRupiah, String winnerId) {}
    LiveAuctionState getAuctionLiveState(UUID listingId);

    void addToExpirySet(UUID listingId, long endTimeEpochMillis);
    void removeFromExpirySet(UUID listingId);
    java.util.Set<String> getExpiredFromExpirySet(long upToEpochMillis);
}

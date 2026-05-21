package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.Bid;

public interface BidRepositoryPort {
    Bid save(Bid bid);
    Optional<Bid> findById(UUID bidId);
    List<Bid> findByListing(UUID listingId);
    List<Bid> findByBidder(UUID bidderId);

    int countByListing(UUID listingId);

    List<Bid> findByListingAndBidder(UUID listingId, UUID bidderId);
    Optional<Bid> findTopBid(UUID listingId);
    Optional<Bid> findTopByListingAndBidder(UUID listingId, UUID bidderId);
    Optional<Bid> findPreviousWinningBidByBidder(UUID listingId, UUID bidderId, UUID excludeBidId);

}

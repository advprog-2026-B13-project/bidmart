package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.persistence.spring;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BidSpringRepository extends JpaRepository<Bid, UUID> {
    List<Bid> findByListingIdOrderByAmountDescCreatedAtAsc(UUID listingId);
    List<Bid> findByBidderIdOrderByCreatedAtDesc(UUID bidderId);
    Optional<Bid> findFirstByListingIdOrderByAmountDescCreatedAtAsc(UUID listingId);
    Optional<Bid> findFirstByListingIdAndBidderIdOrderByAmountDescCreatedAtAsc(UUID listingId, UUID bidderId);
    Optional<Bid> findFirstByListingIdAndBidderIdAndIdNotOrderByAmountDescCreatedAtAsc(UUID listingId, UUID bidderId, UUID excludeBidId);
}

package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.persistence.spring;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.Bid;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidStatus;

@Repository
public interface BidSpringRepository extends JpaRepository<Bid, UUID> {

        interface BidderMaxProjection {
                UUID getBidderId();

                BigDecimal getMaxAmount();
        }

        List<Bid> findByListingIdOrderByMaxAmountDescCreatedAtAsc(UUID listingId);

        List<Bid> findByBidderIdOrderByCreatedAtDesc(UUID bidderId);

        @Query("""
                        select b.bidderId as bidderId,
                        max(b.maxAmount) as maxAmount
                        from Bid b
                        where b.listingId = :listingId and b.status = 'ACCEPTED'
                        group by b.bidderId
                        order by max(b.maxAmount) desc
                        """)
        List<BidderMaxProjection> findTopBidderMax(@Param("listingId") UUID listingId);

        Optional<Bid> findFirstByListingIdAndStatusOrderByMaxAmountDescCreatedAtAsc(
                        UUID listingId,
                        BidStatus status);

        Optional<Bid> findFirstByListingIdAndBidderIdOrderByMaxAmountDescCreatedAtAsc(
                        UUID listingId,
                        UUID bidderId);

        Optional<Bid> findFirstByListingIdAndBidderIdAndStatusAndIdNotOrderByMaxAmountDescCreatedAtAsc(
                        UUID listingId,
                        UUID bidderId,
                        BidStatus status,
                        UUID excludeBidId);

        List<Bid> findByListingIdAndBidderIdOrderByMaxAmountDescCreatedAtAsc(UUID listingId, UUID bidderId);

}

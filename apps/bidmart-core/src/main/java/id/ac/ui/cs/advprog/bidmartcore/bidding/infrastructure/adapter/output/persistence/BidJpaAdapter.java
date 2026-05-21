package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.Bid;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidStatus;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.BidRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.persistence.spring.BidSpringRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BidJpaAdapter implements BidRepositoryPort {

    private final BidSpringRepository bidRepository;

    @Override
    public Bid save(Bid bid) {
        return bidRepository.save(bid);
    }

    @Override
    public Optional<Bid> findById(UUID bidId) {
        return bidRepository.findById(bidId);
    }

    @Override
    public List<Bid> findByListing(UUID listingId) {
        return bidRepository.findByListingIdOrderByMaxAmountDescCreatedAtAsc(listingId);
    }

    @Override
    public List<Bid> findByBidder(UUID bidderId) {
        return bidRepository.findByBidderIdOrderByCreatedAtDesc(bidderId);
    }

    @Override
    public int countByListing(UUID listingId) {
        return bidRepository.countByListingId(listingId);
    }

    @Override
    public List<Bid> findByListingAndBidder(UUID listingId, UUID bidderId) {
        return bidRepository.findByListingIdAndBidderIdOrderByMaxAmountDescCreatedAtAsc(listingId, bidderId);
    }

    @Override
    public Optional<Bid> findTopBid(UUID listingId) {
        return bidRepository.findFirstByListingIdAndStatusOrderByMaxAmountDescCreatedAtAsc(listingId,
                BidStatus.ACCEPTED);
    }

    @Override
    public Optional<Bid> findTopByListingAndBidder(UUID listingId, UUID bidderId) {
        return bidRepository.findFirstByListingIdAndBidderIdOrderByMaxAmountDescCreatedAtAsc(listingId, bidderId);
    }

    @Override
    public Optional<Bid> findPreviousWinningBidByBidder(UUID listingId, UUID bidderId, UUID excludeBidId) {
        return bidRepository.findFirstByListingIdAndBidderIdAndStatusAndIdNotOrderByMaxAmountDescCreatedAtAsc(
                listingId, bidderId, BidStatus.ACCEPTED, excludeBidId);
    }
}

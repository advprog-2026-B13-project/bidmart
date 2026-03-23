package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.Bid;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.BidRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.persistence.spring.BidSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        return bidRepository.findByListingIdOrderByAmountDescCreatedAtAsc(listingId);
    }

    @Override
    public List<Bid> findByBidder(UUID bidderId) {
        return bidRepository.findByBidderIdOrderByCreatedAtDesc(bidderId);
    }

    @Override
    public Optional<Bid> findTopBid(UUID listingId) {
        List<Bid> bids = bidRepository.findByListingIdOrderByAmountDescCreatedAtAsc(listingId);
        return bids.isEmpty() ? Optional.empty() : Optional.of(bids.get(0));
    }
}

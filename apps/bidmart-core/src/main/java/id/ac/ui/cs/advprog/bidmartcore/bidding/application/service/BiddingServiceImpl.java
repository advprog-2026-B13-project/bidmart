package id.ac.ui.cs.advprog.bidmartcore.bidding.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionClosedEvent;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionClosedEvent.AuctionResult;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.BidPlacedEvent;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.OutbidEvent;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.Bid;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidStatus;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.input.BiddingUseCase;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.BidRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.EventPublisherPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort.ListingInfo;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BiddingServiceImpl implements BiddingUseCase {

    private final BidRepositoryPort bidRepository;
    private final ListingPort listingPort;
    private final EventPublisherPort eventPublisher;
    private final EnglishAuctionValidator validator;

    @Override
    @Transactional
    public BidResult placeBid(UUID listingId, BigDecimal amount, UUID bidderId) {
        // 1. Load listing info
        ListingInfo listing = listingPort.getListingInfo(listingId);

        // 2. Static validation: seller != bidder, amount > 0
        validator.validateStatic(listing.sellerId(), amount, bidderId);

        // 3. Get current top bid for price comparison and outbid detection
        Optional<Bid> previousTopBid = bidRepository.findTopBid(listingId);
        BigDecimal currentPrice = previousTopBid.map(Bid::getAmount).orElse(null);

        // 4. Dynamic validation: status, timing, price threshold
        validator.validateDynamic(listing, amount, currentPrice);

        // 5. Mark previous winning bid as OUTBID (always, even self-bid)
        UUID previousWinnerId = previousTopBid.map(Bid::getBidderId).orElse(null);
        BigDecimal previousAmount = previousTopBid.map(Bid::getAmount).orElse(null);
        if (previousWinnerId != null) {
            previousTopBid.ifPresent(bid -> {
                bid.setStatus(BidStatus.OUTBID);
                bidRepository.save(bid);
            });
        }

        // 6. Save new bid
        Bid bid = new Bid();
        bid.setListingId(listingId);
        bid.setBidderId(bidderId);
        bid.setAmount(amount);
        bid.setStatus(BidStatus.ACCEPTED);
        Bid savedBid = bidRepository.save(bid);

        // 7. Update listing
        listingPort.updateCurrentPriceAndWinner(listingId, amount, bidderId);

        // 8. Publish events
        eventPublisher.publishBidPlaced(new BidPlacedEvent(
                listingId, savedBid.getId(), bidderId, amount, LocalDateTime.now()
        ));

        if (previousWinnerId != null && !previousWinnerId.equals(bidderId)) {
            eventPublisher.publishOutbid(new OutbidEvent(
                    listingId, previousWinnerId, previousAmount, amount, LocalDateTime.now()
            ));
        }

        return new BidResult(
                savedBid.getId(),
                listingId,
                bidderId,
                amount,
                BidStatus.ACCEPTED.name(),
                savedBid.getCreatedAt()
        );
    }

    @Override
    public List<BidResult> getBidsForListing(UUID listingId) {
        return bidRepository.findByListing(listingId).stream()
                .map(bid -> new BidResult(
                        bid.getId(),
                        bid.getListingId(),
                        bid.getBidderId(),
                        bid.getAmount(),
                        bid.getStatus().name(),
                        bid.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public List<BidResult> getMyBids(UUID bidderId) {
        return bidRepository.findByBidder(bidderId).stream()
                .map(bid -> new BidResult(
                        bid.getId(),
                        bid.getListingId(),
                        bid.getBidderId(),
                        bid.getAmount(),
                        bid.getStatus().name(),
                        bid.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public AuctionStatusResult getAuctionStatus(UUID listingId) {
        ListingInfo listing = listingPort.getListingInfo(listingId);
        Optional<Bid> topBid = bidRepository.findTopBid(listingId);

        return new AuctionStatusResult(
                listingId,
                listing.currentPrice(),
                topBid.map(Bid::getBidderId).orElse(null),
                null,
                listing.endTime(),
                listing.status().name()
        );
    }

    @Override
    @Transactional
    public void closeAuction(UUID listingId) {
        ListingInfo listing = listingPort.getListingInfo(listingId);

        if (listing.status() != ListingStatus.ACTIVE) {
            return;
        }

        Optional<Bid> topBid = bidRepository.findTopBid(listingId);
        LocalDateTime now = LocalDateTime.now();

        if (topBid.isPresent() && topBid.get().getAmount().compareTo(listing.startingPrice()) >= 0) {
            Bid winnerBid = topBid.get();
            listingPort.updateStatus(listingId, ListingStatus.WON);
            listingPort.updateCurrentPriceAndWinner(listingId, winnerBid.getAmount(), winnerBid.getBidderId());

            winnerBid.setStatus(BidStatus.WON);
            bidRepository.save(winnerBid);

            eventPublisher.publishAuctionClosed(new AuctionClosedEvent(
                    listingId,
                    listing.sellerId(),
                    winnerBid.getBidderId(),
                    winnerBid.getAmount(),
                    AuctionResult.WON,
                    now
            ));
        } else {
            listingPort.updateStatus(listingId, ListingStatus.UNSOLD);

            eventPublisher.publishAuctionClosed(new AuctionClosedEvent(
                    listingId,
                    listing.sellerId(),
                    null,
                    null,
                    AuctionResult.UNSOLD,
                    now
            ));
        }
    }
}

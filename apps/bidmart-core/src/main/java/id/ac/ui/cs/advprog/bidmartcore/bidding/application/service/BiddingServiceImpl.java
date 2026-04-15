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
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionTimeExtendedEvent;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.BidPlacedEvent;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.OutbidEvent;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.Bid;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidStatus;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.ConcurrencyResult;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.ConcurrencyResult.BidAcceptance;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.input.BiddingUseCase;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.BidRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ConcurrencyPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.EventPublisherPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort.ListingInfo;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.WalletPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.config.BiddingProperties;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BiddingServiceImpl implements BiddingUseCase {

    private final BidRepositoryPort bidRepository;
    private final ListingPort listingPort;
    private final EventPublisherPort eventPublisher;
    private final ConcurrencyPort concurrencyPort;
    private final WalletPort walletPort;
    private final EnglishAuctionValidator validator;
    private final BiddingProperties properties;

    @Override
    @Transactional
    public BidResult placeBid(UUID listingId, BigDecimal amount, UUID bidderId) {
        ListingInfo listing = listingPort.getListingInfo(listingId);

        // Static validation
        validator.validateStatic(listing.sellerId(), amount, bidderId);

        // Reserve funds before mutating auction state.
        try {
            walletPort.holdFunds(bidderId, amount);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Saldo tidak mencukupi");
        } catch (Exception e) {
            throw new IllegalStateException("Gagal menahan saldo");
        }

        // Redis atomic execution with bounded retry
        long bidRupiah = amount.longValue();
        long minIncrementRupiah = listing.minBidIncrement().longValue();
        long antiSnipeThresholdMillis = properties.getAntiSnipeSecondsBeforeClose() * 1000L;
        long antiSnipeExtensionMillis = properties.getAntiSnipeExtensionSeconds() * 1000L;

        int retries = 0;
        ConcurrencyResult result = null;
        while (retries < 3) {
            long nowMillis = System.currentTimeMillis();
            result = concurrencyPort.placeBid(
                    listingId, bidRupiah, minIncrementRupiah, bidderId,
                    nowMillis, antiSnipeThresholdMillis, antiSnipeExtensionMillis);

            if (result.status() == BidAcceptance.CACHE_MISS) {
                concurrencyPort.cacheAuction(listingId, listing);
                retries++;
                continue;
            }
            break;
        }

        if (result == null || result.status() == BidAcceptance.CACHE_MISS) {
            walletPort.releaseFunds(bidderId, amount);
            throw new IllegalStateException("System unavailable, please try again.");
        }

        if (result.status() != BidAcceptance.ACCEPTED) {
            walletPort.releaseFunds(bidderId, amount);
            switch (result.status()) {
                case REJECTED -> throw new IllegalArgumentException("Penawaran terlalu rendah");
                case NOT_ACTIVE -> throw new IllegalArgumentException("Lelang tidak aktif");
                case ENDED -> throw new IllegalArgumentException("Lelang sudah berakhir");
                default -> throw new IllegalArgumentException("Penawaran ditolak");
            }
        }

        // Cold storage
        try {
            Bid bid = new Bid();
            bid.setListingId(listingId);
            bid.setBidderId(bidderId);
            bid.setAmount(amount);
            bid.setStatus(BidStatus.ACCEPTED);
            Bid savedBid = bidRepository.save(bid);

            // Mark previous winning bid as OUTBID
            if (result.oldWinner() != null && !result.oldWinner().isEmpty()) {
                UUID outbidBidderId = UUID.fromString(result.oldWinner());
                bidRepository
                        .findPreviousWinningBidByBidder(listingId, outbidBidderId, savedBid.getId())
                        .ifPresent(prevBid -> {
                            prevBid.setStatus(BidStatus.OUTBID);
                            bidRepository.save(prevBid);
                        });
            }

            eventPublisher.publishBidPlaced(new BidPlacedEvent(
                    listingId, savedBid.getId(), bidderId, amount, LocalDateTime.now()));

            // Sync extended endTime back to PSQL
            long currentEndTimeMillis = concurrencyPort.getAuctionEndTime(listingId);
            if (currentEndTimeMillis != result.oldEndTime()) {
                LocalDateTime newEndTime = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(currentEndTimeMillis),
                        java.time.ZoneId.of("Asia/Jakarta"));
                LocalDateTime previousEndTime = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(result.oldEndTime()),
                        java.time.ZoneId.of("Asia/Jakarta"));
                eventPublisher.publishAuctionTimeExtended(
                        new AuctionTimeExtendedEvent(listingId, previousEndTime, newEndTime));
            }

            // Only alert previous winner if they are different from the new winner
            if (result.oldWinner() != null && !result.oldWinner().isEmpty()
                    && !result.oldWinner().equals(bidderId.toString())) {
                eventPublisher.publishOutbid(new OutbidEvent(
                        listingId,
                        UUID.fromString(result.oldWinner()),
                        BigDecimal.valueOf(result.oldPrice()),
                        amount,
                        LocalDateTime.now()));
            }

            return new BidResult(
                    savedBid.getId(), listingId, bidderId, amount,
                    BidStatus.ACCEPTED.name(), savedBid.getCreatedAt());

        } catch (Exception e) {
            // Rollback wallet hold and Redis state
            walletPort.releaseFunds(bidderId, amount);
            concurrencyPort.rollback(
                    listingId,
                    result.oldPrice(),
                    result.oldWinner(),
                    result.oldEndTime(),
                    bidRupiah,
                    bidderId.toString());
            throw e;
        }
    }

    @Override
    public List<BidResult> getBidsForListing(UUID listingId) {
        return bidRepository.findByListing(listingId).stream()
                .map(bid -> new BidResult(
                        bid.getId(), bid.getListingId(), bid.getBidderId(),
                        bid.getAmount(), bid.getStatus().name(), bid.getCreatedAt()))
                .toList();
    }

    @Override
    public List<BidResult> getMyBids(UUID bidderId) {
        return bidRepository.findByBidder(bidderId).stream()
                .map(bid -> new BidResult(
                        bid.getId(), bid.getListingId(), bid.getBidderId(),
                        bid.getAmount(), bid.getStatus().name(), bid.getCreatedAt()))
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
                listing.endTime(),
                listing.status().name());
    }

    @Override
    @Transactional
    public void closeAuction(UUID listingId) {
        ListingInfo listing = listingPort.getListingInfo(listingId);

        if (listing.status() != ListingStatus.ACTIVE) {
            return;
        }

        concurrencyPort.removeAuction(listingId);

        Optional<Bid> topBid = bidRepository.findTopBid(listingId);
        LocalDateTime now = LocalDateTime.now();

        if (topBid.isPresent() && topBid.get().getAmount().compareTo(listing.reservePrice()) >= 0) {
            Bid winnerBid = topBid.get();

            winnerBid.setStatus(BidStatus.WON);
            bidRepository.save(winnerBid);

            eventPublisher.publishAuctionClosed(new AuctionClosedEvent(
                    listingId, listing.sellerId(), winnerBid.getBidderId(),
                    winnerBid.getAmount(), AuctionResult.WON, now));
        } else {
            topBid.ifPresent(bid -> {
                bid.setStatus(BidStatus.OUTBID);
                bidRepository.save(bid);
            });

            eventPublisher.publishAuctionClosed(new AuctionClosedEvent(
                    listingId, listing.sellerId(), null,
                    null, AuctionResult.UNSOLD, now));
        }
    }
}

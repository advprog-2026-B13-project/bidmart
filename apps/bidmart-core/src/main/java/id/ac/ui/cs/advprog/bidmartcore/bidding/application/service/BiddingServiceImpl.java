package id.ac.ui.cs.advprog.bidmartcore.bidding.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidSource;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidStatus;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidType;
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
    private final AuctionValidator validator;
    private final BiddingProperties properties;

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Jakarta");
    private static final int MAX_CACHE_RETRIES = 3;

    private record BidProcessingOutcome(BidResult bidResult, List<Runnable> publishActions) {
    }

    @Override
    @Transactional
    public BidResult placeBid(UUID listingId, BigDecimal bidAmount, UUID bidderId, BidType bidType) {
        ListingInfo listing = listingPort.getListingInfo(listingId);
        validateAndHoldFunds(listing, bidAmount, bidderId);

        long bidRupiah = bidAmount.longValue();
        long minIncrementRupiah = listing.minBidIncrement().longValue();
        ConcurrencyResult result = runRedisWithRetry(listingId, listing, bidRupiah, minIncrementRupiah, bidderId,
                bidType);

        if (result == null || result.status() == BidAcceptance.CACHE_MISS) {
            walletPort.releaseFunds(bidderId, bidAmount);
            throw new IllegalStateException("System unavailable, please try again.");
        }

        boolean stateMutated = result.status() == BidAcceptance.LEADING || result.status() == BidAcceptance.OUTBID;

        try {
            BidProcessingOutcome outcome = switch (result.status()) {
                case LEADING -> handleLeading(listingId, listing, bidderId, bidAmount, result);
                case OUTBID -> handleOutbid(listingId, listing, bidderId, bidAmount, result);
                default -> handleReject(bidderId, bidAmount, result.status());
            };

            outcome.publishActions().forEach(Runnable::run);
            return outcome.bidResult();
        } catch (Exception e) {
            if (stateMutated) {
                compensate(listingId, listing, bidderId, bidAmount, result);
            }
            throw e;
        }
    }

    private void validateAndHoldFunds(ListingInfo listing, BigDecimal amount, UUID bidderId) {
        validator.validateStatic(listing.sellerId(), amount, bidderId);

        try {
            walletPort.holdFunds(bidderId, amount);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Saldo tidak mencukupi");
        } catch (Exception e) {
            throw new IllegalStateException("Gagal menahan saldo");
        }
    }

    private ConcurrencyResult runRedisWithRetry(UUID listingId,
            ListingInfo listing,
            long bidRupiah,
            long minIncrementRupiah,
            UUID bidderId,
            BidType bidType) {
        long antiSnipeThresholdMillis = properties.getAntiSnipeSecondsBeforeClose() * 1000L;
        long antiSnipeExtensionMillis = properties.getAntiSnipeExtensionSeconds() * 1000L;

        int retries = 0;
        ConcurrencyResult result = null;
        while (retries < MAX_CACHE_RETRIES) {
            long nowMillis = System.currentTimeMillis();
            result = concurrencyPort.placeBid(
                    listingId,
                    bidRupiah,
                    minIncrementRupiah,
                    bidderId,
                    bidType,
                    nowMillis,
                    antiSnipeThresholdMillis,
                    antiSnipeExtensionMillis);

            if (result.status() == BidAcceptance.CACHE_MISS) {
                concurrencyPort.cacheAuction(listingId, listing);
                retries++;
                continue;
            }
            break;
        }

        return result;
    }

    private BidProcessingOutcome handleLeading(UUID listingId,
            ListingInfo listing,
            UUID bidderId,
            BigDecimal submittedAmount,
            ConcurrencyResult result) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal visiblePrice = BigDecimal.valueOf(result.visiblePrice());
        List<Runnable> publishActions = new ArrayList<>();

        Bid previousWinningBid = markPreviousWinningBidAsOutbid(listingId);

        Bid savedBid = saveBid(listingId, bidderId, visiblePrice, submittedAmount, BidSource.MANUAL,
                BidStatus.ACCEPTED);
        BidPlacedEvent bidPlacedEvent = new BidPlacedEvent(
                listingId,
                savedBid.getId(),
                bidderId,
                visiblePrice,
                now);
        publishActions.add(() -> eventPublisher.publishBidPlaced(bidPlacedEvent));

        AuctionTimeExtendedEvent extensionEvent = buildAuctionExtensionEventIfNeeded(
                listingId,
                listing.endTime(),
                result.endTime());
        if (extensionEvent != null) {
            publishActions.add(() -> eventPublisher.publishAuctionTimeExtended(extensionEvent));
        }

        if (previousWinningBid != null && !previousWinningBid.getBidderId().equals(bidderId)) {
            OutbidEvent outbidEvent = new OutbidEvent(
                    listingId,
                    previousWinningBid.getBidderId(),
                    previousWinningBid.getAmount(),
                    visiblePrice,
                    now);
            publishActions.add(() -> eventPublisher.publishOutbid(outbidEvent));
        }

        return new BidProcessingOutcome(toBidResult(savedBid), publishActions);
    }

    private BidProcessingOutcome handleOutbid(UUID listingId,
            ListingInfo listing,
            UUID bidderId,
            BigDecimal submittedAmount,
            ConcurrencyResult result) {
        walletPort.releaseFunds(bidderId, submittedAmount);

        markPreviousWinningBidAsOutbid(listingId);

        LocalDateTime now = LocalDateTime.now();
        BigDecimal visiblePrice = BigDecimal.valueOf(result.visiblePrice());
        List<Runnable> publishActions = new ArrayList<>();
        Bid savedBid = saveBid(
                listingId,
                bidderId,
                visiblePrice,
                submittedAmount,
                BidSource.MANUAL,
                BidStatus.OUTBID);

        BidPlacedEvent bidPlacedEvent = new BidPlacedEvent(
                listingId,
                savedBid.getId(),
                bidderId,
                visiblePrice,
                now);
        publishActions.add(() -> eventPublisher.publishBidPlaced(bidPlacedEvent));

        if (result.proxyVisiblePrice() != null && result.proxyWinnerId() != null && !result.proxyWinnerId().isBlank()) {
            UUID proxyWinnerId = UUID.fromString(result.proxyWinnerId());
            BigDecimal proxyVisiblePrice = BigDecimal.valueOf(result.proxyVisiblePrice());
            Bid proxyBid = saveBid(
                    listingId,
                    proxyWinnerId,
                    proxyVisiblePrice,
                    proxyVisiblePrice,
                    BidSource.PROXY,
                    BidStatus.ACCEPTED);

            BidPlacedEvent proxyBidPlacedEvent = new BidPlacedEvent(
                    listingId,
                    proxyBid.getId(),
                    proxyWinnerId,
                    proxyVisiblePrice,
                    now);
            publishActions.add(() -> eventPublisher.publishBidPlaced(proxyBidPlacedEvent));
        }

        AuctionTimeExtendedEvent extensionEvent = buildAuctionExtensionEventIfNeeded(
                listingId,
                listing.endTime(),
                result.endTime());
        if (extensionEvent != null) {
            publishActions.add(() -> eventPublisher.publishAuctionTimeExtended(extensionEvent));
        }

        return new BidProcessingOutcome(toBidResult(savedBid), publishActions);
    }

    private Bid markPreviousWinningBidAsOutbid(UUID listingId) {
        Bid previousWinningBid = bidRepository.findTopBid(listingId).orElse(null);
        if (previousWinningBid != null) {
            previousWinningBid.setStatus(BidStatus.OUTBID);
            bidRepository.save(previousWinningBid);
        }

        return previousWinningBid;
    }

    private BidProcessingOutcome handleReject(UUID bidderId, BigDecimal submittedAmount, BidAcceptance status) {
        walletPort.releaseFunds(bidderId, submittedAmount);
        switch (status) {
            case REJECTED -> throw new IllegalArgumentException("Penawaran terlalu rendah");
            case NOT_ACTIVE -> throw new IllegalArgumentException("Lelang tidak aktif");
            case ENDED -> throw new IllegalArgumentException("Lelang sudah berakhir");
            default -> throw new IllegalArgumentException("Penawaran ditolak");
        }
    }

    private void compensate(UUID listingId,
            ListingInfo listing,
            UUID bidderId,
            BigDecimal submittedAmount,
            ConcurrencyResult result) {
        if (result.status() == BidAcceptance.LEADING) {
            walletPort.releaseFunds(bidderId, submittedAmount);
        }

        long priceToRestore = resolveListingPrice(listing);
        String winnerToRestore = listing.winnerId() != null ? listing.winnerId().toString() : "";
        long endTimeToRestore = toEpochMillis(listing.endTime());
        String expectedCurrentWinner = result.winnerId() != null ? result.winnerId() : "";
        long expectedCurrentPrice = resolveExpectedCurrentPrice(result);

        concurrencyPort.rollback(
                listingId,
                priceToRestore,
                winnerToRestore,
                endTimeToRestore,
                expectedCurrentPrice,
                expectedCurrentWinner);
    }

    private long resolveExpectedCurrentPrice(ConcurrencyResult result) {
        // OUTBID returns challenger visible price and winner counter visible price.
        if (result.status() == BidAcceptance.OUTBID && result.proxyVisiblePrice() != null) {
            return result.proxyVisiblePrice();
        }
        return result.visiblePrice();
    }

    private Bid saveBid(UUID listingId,
            UUID bidderId,
            BigDecimal visibleAmount,
            BigDecimal maxAmount,
            BidSource bidSource,
            BidStatus status) {
        Bid bid = new Bid();
        bid.setListingId(listingId);
        bid.setBidderId(bidderId);
        bid.setAmount(visibleAmount);
        bid.setMaxAmount(maxAmount);
        bid.setSource(bidSource);
        bid.setStatus(status);
        return bidRepository.save(bid);
    }

    private AuctionTimeExtendedEvent buildAuctionExtensionEventIfNeeded(UUID listingId,
            LocalDateTime previousEndTime,
            long newEndTimeMillis) {
        if (toEpochMillis(previousEndTime) == newEndTimeMillis) {
            return null;
        }

        LocalDateTime newEndTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(newEndTimeMillis), BUSINESS_ZONE);
        return new AuctionTimeExtendedEvent(listingId, previousEndTime, newEndTime);
    }

    private long resolveListingPrice(ListingInfo listing) {
        if (listing.currentPrice() != null) {
            return listing.currentPrice().longValue();
        }
        return listing.startingPrice().longValue();
    }

    private long toEpochMillis(LocalDateTime time) {
        return time.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
    }

    private BidResult toBidResult(Bid bid) {
        return new BidResult(
                bid.getId(),
                bid.getListingId(),
                bid.getBidderId(),
                bid.getAmount(),
                bid.getStatus().name(),
                bid.getCreatedAt());
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

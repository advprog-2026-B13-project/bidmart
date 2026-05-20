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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

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
import id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.redis.RedisAuctionEventPublisher;
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
    private final RedisAuctionEventPublisher auctionEventPublisher;
    private final MeterRegistry meterRegistry;

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Jakarta");
    private static final int MAX_CACHE_RETRIES = 3;

    private record BidProcessingOutcome(BidResult bidResult, List<Runnable> publishActions) {
    }

    @Override
    @Transactional
    public BidResult placeBid(UUID listingId, BigDecimal bidAmount, UUID bidderId, BidType bidType) {
        Timer.Sample totalSample = Timer.start(meterRegistry);

        Timer.Sample listingReadSample = Timer.start(meterRegistry);
        ListingInfo listing = listingPort.getListingInfo(listingId);
        listingReadSample.stop(meterRegistry.timer("bidding.listing_read"));

        Timer.Sample walletSample = Timer.start(meterRegistry);
        validateAndHoldFunds(listing, bidAmount, bidderId);
        walletSample.stop(meterRegistry.timer("bidding.wallet_hold"));

        long bidRupiah = bidAmount.longValue();
        long minIncrementRupiah = listing.minBidIncrement().longValue();

        Timer.Sample redisSample = Timer.start(meterRegistry);
        ConcurrencyResult result = runRedisWithRetry(listingId, listing, bidRupiah, minIncrementRupiah, bidderId,
                bidType);
        redisSample.stop(meterRegistry.timer("bidding.redis_decision"));

        if (result == null || result.status() == BidAcceptance.CACHE_MISS) {
            walletPort.releaseFunds(bidderId, bidAmount);
            totalSample.stop(meterRegistry.timer("bidding.place_bid", "outcome", "cache_miss"));
            throw new IllegalStateException("System unavailable, please try again.");
        }

        boolean stateMutated = result.status() == BidAcceptance.LEADING || result.status() == BidAcceptance.OUTBID;

        try {
            Timer.Sample dbWriteSample = Timer.start(meterRegistry);
            BidProcessingOutcome outcome = switch (result.status()) {
                case LEADING -> handleLeading(listingId, listing, bidderId, bidAmount, result, bidType);
                case OUTBID -> handleOutbid(listingId, listing, bidderId, bidAmount, result);
                default -> handleReject(bidderId, bidAmount, result.status());
            };
            dbWriteSample.stop(meterRegistry.timer("bidding.db_write",
                    "outcome", result.status().name().toLowerCase()));

            outcome.publishActions().forEach(Runnable::run);

            totalSample.stop(meterRegistry.timer("bidding.place_bid",
                    "outcome", result.status().name().toLowerCase()));
            return outcome.bidResult();
        } catch (Exception e) {
            if (stateMutated) {
                compensate(listingId, listing, bidderId, bidAmount, result);
            }
            totalSample.stop(meterRegistry.timer("bidding.place_bid", "outcome", "error"));
            throw e;
        }
    }

    private void validateAndHoldFunds(ListingInfo listing, BigDecimal amount, UUID bidderId) {
        validator.validateStatic(listing.sellerId(), amount, bidderId);
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        if (listing.startTime() != null && now.isBefore(listing.startTime())) {
            throw new IllegalStateException("Lelang belum dimulai");
        }

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
            ConcurrencyResult result,
            BidType bidType) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal visiblePrice = BigDecimal.valueOf(result.visiblePrice());
        List<Runnable> publishActions = new ArrayList<>();

        Bid currentWinningBid = bidRepository.findTopBid(listingId).orElse(null);

        final Bid activeBid;
        final Bid outbidVictim;

        if (currentWinningBid != null && currentWinningBid.getBidderId().equals(bidderId)) {
            activeBid = applySameBidderLeading(listingId, bidderId, submittedAmount, visiblePrice, bidType, currentWinningBid);
            outbidVictim = null;
        } else {
            outbidVictim = currentWinningBid;
            activeBid = applyChallengerLeading(listingId, bidderId, submittedAmount, visiblePrice, bidType, currentWinningBid);
        }

        BidPlacedEvent bidPlacedEvent = new BidPlacedEvent(
                listingId, activeBid.getId(), bidderId, visiblePrice, now);
        publishActions.add(() -> eventPublisher.publishBidPlaced(bidPlacedEvent));

        int bidCount = bidRepository.countByListing(listingId);
        publishActions.add(() -> auctionEventPublisher.publishPriceChange(listingId, visiblePrice, bidCount));

        AuctionTimeExtendedEvent extensionEvent = buildAuctionExtensionEventIfNeeded(
                listingId, listing.endTime(), result.endTime());
        if (extensionEvent != null) {
            publishActions.add(() -> eventPublisher.publishAuctionTimeExtended(extensionEvent));
        }

        if (outbidVictim != null) {
            OutbidEvent outbidEvent = new OutbidEvent(
                    listingId, outbidVictim.getBidderId(), outbidVictim.getAmount(),
                    visiblePrice, now);
            publishActions.add(() -> eventPublisher.publishOutbid(outbidEvent));
        }

        return new BidProcessingOutcome(toBidResult(activeBid), publishActions);
    }

    private Bid applySameBidderLeading(UUID listingId, UUID bidderId,
            BigDecimal submittedAmount, BigDecimal visiblePrice,
            BidType bidType, Bid winner) {
        if (bidType == BidType.PROXY) {
            return raiseSameBidderProxyMax(bidderId, submittedAmount, winner);
        }
        if (BidSource.PROXY.equals(winner.getSource())
                && winner.getMaxAmount() != null
                && submittedAmount.compareTo(winner.getMaxAmount()) < 0) {
            return manualUnderProxyMax(listingId, bidderId, submittedAmount, visiblePrice, winner);
        }
        return manualAboveOrEqualProxyMax(listingId, bidderId, submittedAmount, visiblePrice, bidType, winner);
    }

    private Bid raiseSameBidderProxyMax(UUID bidderId, BigDecimal submittedAmount, Bid winner) {
        BigDecimal oldMax = winner.getMaxAmount() != null ? winner.getMaxAmount() : winner.getAmount();
        walletPort.releaseFunds(bidderId, oldMax);
        winner.setStatus(BidStatus.ACCEPTED);
        winner.setSource(BidSource.PROXY);
        winner.setMaxAmount(submittedAmount);
        return bidRepository.save(winner);
    }

    private Bid manualUnderProxyMax(UUID listingId, UUID bidderId,
            BigDecimal submittedAmount, BigDecimal visiblePrice, Bid winner) {
        BigDecimal originalProxyMax = winner.getMaxAmount();
        walletPort.releaseFunds(bidderId, submittedAmount);
        winner.setStatus(BidStatus.OUTBID);
        bidRepository.save(winner);
        return saveBid(listingId, bidderId, visiblePrice, originalProxyMax, BidSource.PROXY, BidStatus.ACCEPTED);
    }

    private Bid manualAboveOrEqualProxyMax(UUID listingId, UUID bidderId,
            BigDecimal submittedAmount, BigDecimal visiblePrice,
            BidType bidType, Bid winner) {
        BigDecimal prevHeld = winner.getMaxAmount() != null ? winner.getMaxAmount() : winner.getAmount();
        winner.setStatus(BidStatus.OUTBID);
        bidRepository.save(winner);
        walletPort.releaseFunds(bidderId, prevHeld);
        BidSource bidSource = bidType == BidType.PROXY ? BidSource.PROXY : BidSource.MANUAL;
        return saveBid(listingId, bidderId, visiblePrice, submittedAmount, bidSource, BidStatus.ACCEPTED);
    }

    private Bid applyChallengerLeading(UUID listingId, UUID bidderId,
            BigDecimal submittedAmount, BigDecimal visiblePrice,
            BidType bidType, Bid previousWinner) {
        if (previousWinner != null) {
            BigDecimal prevHeld = previousWinner.getMaxAmount() != null
                    ? previousWinner.getMaxAmount() : previousWinner.getAmount();
            previousWinner.setStatus(BidStatus.OUTBID);
            bidRepository.save(previousWinner);
            walletPort.releaseFunds(previousWinner.getBidderId(), prevHeld);
        }
        BidSource bidSource = bidType == BidType.PROXY ? BidSource.PROXY : BidSource.MANUAL;
        return saveBid(listingId, bidderId, visiblePrice, submittedAmount, bidSource, BidStatus.ACCEPTED);
    }

    private BidProcessingOutcome handleOutbid(UUID listingId,
            ListingInfo listing,
            UUID bidderId,
            BigDecimal submittedAmount,
            ConcurrencyResult result) {
        walletPort.releaseFunds(bidderId, submittedAmount);

        Bid previousWinningBid = markPreviousWinningBidAsOutbid(listingId);

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
            // Preserve the original proxy max so future outbid releases the correct held amount.
            BigDecimal originalProxyMax = previousWinningBid != null
                    && previousWinningBid.getMaxAmount() != null
                    ? previousWinningBid.getMaxAmount()
                    : proxyVisiblePrice;
            Bid proxyBid = saveBid(
                    listingId,
                    proxyWinnerId,
                    proxyVisiblePrice,
                    originalProxyMax,
                    BidSource.PROXY,
                    BidStatus.ACCEPTED);

            BidPlacedEvent proxyBidPlacedEvent = new BidPlacedEvent(
                    listingId,
                    proxyBid.getId(),
                    proxyWinnerId,
                    proxyVisiblePrice,
                    now);
            publishActions.add(() -> eventPublisher.publishBidPlaced(proxyBidPlacedEvent));

            // SSE: proxy counter changed the visible price.
            int bidCount = bidRepository.countByListing(listingId);
            publishActions.add(() -> auctionEventPublisher.publishPriceChange(
                    listingId, proxyVisiblePrice, bidCount));
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
                bid.getMaxAmount(),
                bid.getSource() != null ? bid.getSource().name() : null,
                bid.getStatus().name(),
                bid.getCreatedAt());
    }

    @Override
    public List<BidResult> getBidsForListing(UUID listingId) {
        return bidRepository.findByListing(listingId).stream()
                .map(this::toBidResult)
                .toList();
    }

    @Override
    public List<BidResult> getMyBids(UUID bidderId) {
        return bidRepository.findByBidder(bidderId).stream()
                .map(this::toBidResult)
                .toList();
    }

    @Override
    public AuctionStatusResult getAuctionStatus(UUID listingId) {
        ListingInfo listing = listingPort.getListingInfo(listingId);

        BigDecimal livePrice = listing.currentPrice();
        UUID liveWinnerId = null;

        ConcurrencyPort.LiveAuctionState live = concurrencyPort.getAuctionLiveState(listingId);
        if (live != null) {
            livePrice = BigDecimal.valueOf(live.priceRupiah());
            liveWinnerId = live.winnerId() != null && !live.winnerId().isBlank()
                    ? UUID.fromString(live.winnerId()) : null;
        } else {
            Optional<Bid> topBid = bidRepository.findTopBid(listingId);
            liveWinnerId = topBid.map(Bid::getBidderId).orElse(null);
        }

        return new AuctionStatusResult(listingId, livePrice, liveWinnerId,
                listing.endTime(), listing.status().name());
    }

    @Override
    @Transactional
    public void closeAuction(UUID listingId) {
        // Always clean Redis first — prevents stale expiry set entries
        concurrencyPort.removeAuction(listingId);

        ListingInfo listing = listingPort.getListingInfo(listingId);
        if (listing.status() != ListingStatus.ACTIVE) {
            return;
        }

        Optional<Bid> topBid = bidRepository.findTopBid(listingId);
        LocalDateTime now = LocalDateTime.now();

        if (topBid.isPresent() && topBid.get().getAmount().compareTo(listing.reservePrice()) >= 0) {
            Bid winnerBid = topBid.get();

            winnerBid.setStatus(BidStatus.WON);
            bidRepository.save(winnerBid);

            eventPublisher.publishAuctionClosed(new AuctionClosedEvent(
                    listingId, listing.sellerId(), winnerBid.getBidderId(),
                    winnerBid.getAmount(), AuctionResult.WON, now));

            auctionEventPublisher.publishAuctionEnded(listingId, winnerBid.getBidderId(),
                    winnerBid.getAmount(), "WON");
        } else {
            topBid.ifPresent(bid -> {
                bid.setStatus(BidStatus.OUTBID);
                bidRepository.save(bid);
            });

            eventPublisher.publishAuctionClosed(new AuctionClosedEvent(
                    listingId, listing.sellerId(), null,
                    null, AuctionResult.UNSOLD, now));

            auctionEventPublisher.publishAuctionEnded(listingId, null, null, "UNSOLD");
        }
    }
}

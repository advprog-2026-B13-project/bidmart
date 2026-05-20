package id.ac.ui.cs.advprog.bidmartcore.bidding.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.Bid;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidSource;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidStatus;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidType;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.ConcurrencyResult;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.input.BiddingUseCase;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.AuctionNotificationPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.BidRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.BiddingMetricsPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.BiddingMetricsPort.Sample;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ConcurrencyPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.EventPublisherPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.WalletPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.config.BiddingProperties;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;

@ExtendWith(MockitoExtension.class)
class BiddingServiceImplTest {

    @Mock private BidRepositoryPort bidRepository;
    @Mock private ListingPort listingPort;
    @Mock private EventPublisherPort eventPublisher;
    @Mock private ConcurrencyPort concurrencyPort;
    @Mock private WalletPort walletPort;
    @Mock private AuctionValidator validator;
    @Mock private AuctionNotificationPort auctionNotifier;
    @Mock private BiddingMetricsPort metrics;

    private BiddingProperties properties;
    private BiddingServiceImpl service;

    private UUID listingId;
    private UUID bidderId;
    private UUID sellerId;
    private ListingPort.ListingInfo activeListing;
    // Derived from activeListing.endTime() so it matches toEpochMillis() exactly
    private long endTimeMillis;

    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");
    private static final Sample DUMMY_SAMPLE = new Sample(0L);

    @BeforeEach
    void setUp() {
        properties = new BiddingProperties();
        service = new BiddingServiceImpl(
                bidRepository, listingPort, eventPublisher, concurrencyPort,
                walletPort, validator, properties, auctionNotifier, metrics);

        listingId = UUID.randomUUID();
        bidderId = UUID.randomUUID();
        sellerId = UUID.randomUUID();

        LocalDateTime endTime = LocalDateTime.now().plusHours(1);
        endTimeMillis = endTime.atZone(JAKARTA).toInstant().toEpochMilli();

        activeListing = new ListingPort.ListingInfo(
                sellerId,
                ListingStatus.ACTIVE,
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(500),
                BigDecimal.ONE,
                null,
                endTime,
                null);

        // lenient: some tests (closeAuction, query methods) don't go through placeBid
        lenient().when(metrics.start()).thenReturn(DUMMY_SAMPLE);
        lenient().when(listingPort.getListingInfo(listingId)).thenReturn(activeListing);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Bid acceptedBid(UUID bidderId, BigDecimal amount, BigDecimal maxAmount,
            BidSource source) {
        Bid bid = new Bid();
        bid.setId(UUID.randomUUID());
        bid.setListingId(listingId);
        bid.setBidderId(bidderId);
        bid.setAmount(amount);
        bid.setMaxAmount(maxAmount);
        bid.setSource(source);
        bid.setStatus(BidStatus.ACCEPTED);
        return bid;
    }

    // ─── Rejection paths ───────────────────────────────────────────────────────

    @Test
    void placeBid_rejected_releasesHoldAndThrows() {
        when(concurrencyPort.placeBid(any(), anyLong(), anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong()))
                .thenReturn(ConcurrencyResult.rejected());

        assertThrows(IllegalArgumentException.class,
                () -> service.placeBid(listingId, BigDecimal.valueOf(5000), bidderId, BidType.MANUAL));

        verify(walletPort).releaseFunds(bidderId, BigDecimal.valueOf(5000));
    }

    @Test
    void placeBid_notActive_releasesHoldAndThrows() {
        when(concurrencyPort.placeBid(any(), anyLong(), anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong()))
                .thenReturn(ConcurrencyResult.notActive());

        assertThrows(IllegalArgumentException.class,
                () -> service.placeBid(listingId, BigDecimal.valueOf(5000), bidderId, BidType.MANUAL));

        verify(walletPort).releaseFunds(bidderId, BigDecimal.valueOf(5000));
    }

    @Test
    void placeBid_ended_releasesHoldAndThrows() {
        when(concurrencyPort.placeBid(any(), anyLong(), anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong()))
                .thenReturn(ConcurrencyResult.ended());

        assertThrows(IllegalArgumentException.class,
                () -> service.placeBid(listingId, BigDecimal.valueOf(5000), bidderId, BidType.MANUAL));

        verify(walletPort).releaseFunds(bidderId, BigDecimal.valueOf(5000));
    }

    // ─── Cache-miss path ───────────────────────────────────────────────────────

    @Test
    void placeBid_cacheMissAllRetries_releasesHoldAndThrowsIllegalState() {
        when(concurrencyPort.placeBid(any(), anyLong(), anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong()))
                .thenReturn(ConcurrencyResult.cacheMiss());

        assertThrows(IllegalStateException.class,
                () -> service.placeBid(listingId, BigDecimal.valueOf(5000), bidderId, BidType.MANUAL));

        verify(walletPort).releaseFunds(bidderId, BigDecimal.valueOf(5000));
        // cacheAuction called once per retry (MAX_CACHE_RETRIES = 3)
        verify(concurrencyPort, times(3)).cacheAuction(eq(listingId), any());
    }

    // ─── LEADING — new challenger, no previous winner ──────────────────────────

    @Test
    void placeBid_leadingNoExistingBid_savesManualAccepted() {
        BigDecimal amount = BigDecimal.valueOf(2000);
        when(concurrencyPort.placeBid(any(), anyLong(), anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong()))
                .thenReturn(ConcurrencyResult.leading(2000L, bidderId.toString(), endTimeMillis, 2000L));
        when(bidRepository.findTopBid(listingId)).thenReturn(Optional.empty());
        when(bidRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.countByListing(listingId)).thenReturn(1);

        BiddingUseCase.BidResult result = service.placeBid(listingId, amount, bidderId, BidType.MANUAL);

        assertEquals("ACCEPTED", result.status());
        assertEquals("MANUAL", result.source());
        // hold was called; no extra release
        verify(walletPort, never()).releaseFunds(any(), any());
    }

    @Test
    void placeBid_leadingNoExistingBid_savesProxyAccepted_whenBidTypeProxy() {
        BigDecimal amount = BigDecimal.valueOf(3000);
        when(concurrencyPort.placeBid(any(), anyLong(), anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong()))
                .thenReturn(ConcurrencyResult.leading(3000L, bidderId.toString(), endTimeMillis, 3000L));
        when(bidRepository.findTopBid(listingId)).thenReturn(Optional.empty());
        when(bidRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.countByListing(listingId)).thenReturn(1);

        BiddingUseCase.BidResult result = service.placeBid(listingId, amount, bidderId, BidType.PROXY);

        assertEquals("ACCEPTED", result.status());
        assertEquals("PROXY", result.source());
    }

    // ─── LEADING — challenger beats an existing leader ─────────────────────────

    @Test
    void placeBid_leadingChallengerBeatsOldLeader_releasesOldLeaderHeld() {
        UUID oldLeaderId = UUID.randomUUID();
        BigDecimal oldMax = BigDecimal.valueOf(1500);
        Bid oldWinner = acceptedBid(oldLeaderId, BigDecimal.valueOf(1000), oldMax, BidSource.PROXY);

        when(concurrencyPort.placeBid(any(), anyLong(), anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong()))
                .thenReturn(ConcurrencyResult.leading(2000L, bidderId.toString(), endTimeMillis, 2000L));
        when(bidRepository.findTopBid(listingId)).thenReturn(Optional.of(oldWinner));
        when(bidRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.countByListing(listingId)).thenReturn(2);

        service.placeBid(listingId, BigDecimal.valueOf(2000), bidderId, BidType.MANUAL);

        verify(walletPort).releaseFunds(oldLeaderId, oldMax);
        verify(walletPort, never()).releaseFunds(eq(bidderId), any());
    }

    // ─── LEADING — same bidder raises proxy max ─────────────────────────────────

    @Test
    void placeBid_sameBidderRaisesProxyMax_releasesOldMaxHoldsNew() {
        BigDecimal oldMax = BigDecimal.valueOf(5000);
        BigDecimal newMax = BigDecimal.valueOf(8000);
        Bid existingProxy = acceptedBid(bidderId, BigDecimal.valueOf(4000), oldMax, BidSource.PROXY);

        when(concurrencyPort.placeBid(any(), anyLong(), anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong()))
                .thenReturn(ConcurrencyResult.leading(8000L, bidderId.toString(), endTimeMillis, 8000L));
        when(bidRepository.findTopBid(listingId)).thenReturn(Optional.of(existingProxy));
        when(bidRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.countByListing(listingId)).thenReturn(1);

        service.placeBid(listingId, newMax, bidderId, BidType.PROXY);

        // releases old max (oldMax was held, newMax already held by holdFunds at top)
        verify(walletPort).releaseFunds(bidderId, oldMax);
        // bid updated to new max
        ArgumentCaptor<Bid> captor = ArgumentCaptor.forClass(Bid.class);
        verify(bidRepository).save(captor.capture());
        assertEquals(newMax, captor.getValue().getMaxAmount());
        assertEquals(BidStatus.ACCEPTED, captor.getValue().getStatus());
    }

    // ─── LEADING — same bidder manual bid under proxy max ──────────────────────

    @Test
    void placeBid_sameBidderManualUnderProxyMax_releaseManualHoldKeepsProxyMax() {
        BigDecimal proxyMax = BigDecimal.valueOf(10000);
        BigDecimal manualAttempt = BigDecimal.valueOf(5500);
        BigDecimal visibleAfter = BigDecimal.valueOf(5501);

        Bid existingProxy = acceptedBid(bidderId, BigDecimal.valueOf(5000), proxyMax, BidSource.PROXY);

        when(concurrencyPort.placeBid(any(), anyLong(), anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong()))
                .thenReturn(ConcurrencyResult.leading(5501L, bidderId.toString(), endTimeMillis, 10000L));
        when(bidRepository.findTopBid(listingId)).thenReturn(Optional.of(existingProxy));
        when(bidRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.countByListing(listingId)).thenReturn(2);

        service.placeBid(listingId, manualAttempt, bidderId, BidType.MANUAL);

        // manual attempt released — proxy max stays held
        verify(walletPort).releaseFunds(bidderId, manualAttempt);
        // new PROXY bid saved with original proxy max
        ArgumentCaptor<Bid> captor = ArgumentCaptor.forClass(Bid.class);
        verify(bidRepository, atLeastOnce()).save(captor.capture());
        List<Bid> saved = captor.getAllValues();
        Bid newProxyBid = saved.stream()
                .filter(b -> BidStatus.ACCEPTED.equals(b.getStatus()))
                .findFirst().orElseThrow();
        assertEquals(BidSource.PROXY, newProxyBid.getSource());
        assertEquals(proxyMax, newProxyBid.getMaxAmount());
    }

    // ─── LEADING — same bidder manual bid above/equal proxy max ────────────────

    @Test
    void placeBid_sameBidderManualAboveProxyMax_releasesPrevHeldSavesManualAccepted() {
        BigDecimal proxyMax = BigDecimal.valueOf(5000);
        BigDecimal manualAmount = BigDecimal.valueOf(6000);

        Bid existingProxy = acceptedBid(bidderId, BigDecimal.valueOf(4000), proxyMax, BidSource.PROXY);

        when(concurrencyPort.placeBid(any(), anyLong(), anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong()))
                .thenReturn(ConcurrencyResult.leading(6000L, bidderId.toString(), endTimeMillis, 6000L));
        when(bidRepository.findTopBid(listingId)).thenReturn(Optional.of(existingProxy));
        when(bidRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.countByListing(listingId)).thenReturn(2);

        service.placeBid(listingId, manualAmount, bidderId, BidType.MANUAL);

        // old proxy max released (bidderId)
        verify(walletPort).releaseFunds(bidderId, proxyMax);
        // new MANUAL ACCEPTED bid
        ArgumentCaptor<Bid> captor = ArgumentCaptor.forClass(Bid.class);
        verify(bidRepository, atLeastOnce()).save(captor.capture());
        Bid newBid = captor.getAllValues().stream()
                .filter(b -> BidStatus.ACCEPTED.equals(b.getStatus()))
                .findFirst().orElseThrow();
        assertEquals(BidSource.MANUAL, newBid.getSource());
        assertEquals(manualAmount, newBid.getMaxAmount());
    }

    // ─── OUTBID — challenger instantly counter-outbid by existing proxy ─────────

    @Test
    void placeBid_outbidByProxy_releasesChallengerHoldSavesProxyBid() {
        UUID proxyOwnerId = UUID.randomUUID();
        BigDecimal challengerAmount = BigDecimal.valueOf(3000);
        long proxyVisibleAfter = 3001L;
        BigDecimal proxyMax = BigDecimal.valueOf(5000);

        Bid existingProxy = acceptedBid(proxyOwnerId, BigDecimal.valueOf(2000), proxyMax, BidSource.PROXY);

        when(concurrencyPort.placeBid(any(), anyLong(), anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong()))
                .thenReturn(ConcurrencyResult.outbid(
                        3000L, proxyOwnerId.toString(), endTimeMillis, 3000L,
                        proxyVisibleAfter, proxyOwnerId.toString()));
        when(bidRepository.findTopBid(listingId)).thenReturn(Optional.of(existingProxy));
        when(bidRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.countByListing(listingId)).thenReturn(2);

        BiddingUseCase.BidResult result = service.placeBid(listingId, challengerAmount, bidderId, BidType.MANUAL);

        // challenger's hold released (they lost)
        verify(walletPort).releaseFunds(bidderId, challengerAmount);
        // challenger bid saved as OUTBID
        assertEquals("OUTBID", result.status());
        // proxy counter-bid published via SSE
        verify(auctionNotifier).publishPriceChange(eq(listingId), any(), anyInt());
    }

    // ─── Seller cannot bid ──────────────────────────────────────────────────────

    @Test
    void placeBid_sellerBidsOwnListing_throwsBeforeHoldingFunds() {
        doThrow(new IllegalArgumentException("Penjual tidak bisa menawar pada lelang sendiri"))
                .when(validator).validateStatic(any(), any(), any());

        assertThrows(IllegalArgumentException.class,
                () -> service.placeBid(listingId, BigDecimal.valueOf(2000), sellerId, BidType.MANUAL));

        verify(walletPort, never()).holdFunds(any(), any());
    }

    // ─── Cache-miss recovers on second attempt ──────────────────────────────────

    @Test
    void placeBid_cacheMissRecoveredOnRetry_succeedsAndCachesOnce() {
        BigDecimal amount = BigDecimal.valueOf(2000);
        when(concurrencyPort.placeBid(any(), anyLong(), anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong()))
                .thenReturn(ConcurrencyResult.cacheMiss())
                .thenReturn(ConcurrencyResult.leading(2000L, bidderId.toString(), endTimeMillis, 2000L));
        when(bidRepository.findTopBid(listingId)).thenReturn(Optional.empty());
        when(bidRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.countByListing(listingId)).thenReturn(1);

        BiddingUseCase.BidResult result = service.placeBid(listingId, amount, bidderId, BidType.MANUAL);

        assertEquals("ACCEPTED", result.status());
        verify(concurrencyPort, times(1)).cacheAuction(eq(listingId), any());
    }

    // ─── Wallet hold failures ───────────────────────────────────────────────────

    @Test
    void placeBid_insufficientBalance_throwsSaldoTidakMencukupi() {
        doThrow(new IllegalArgumentException("Insufficient available balance"))
                .when(walletPort).holdFunds(any(), any());

        assertThrows(IllegalArgumentException.class,
                () -> service.placeBid(listingId, BigDecimal.valueOf(2000), bidderId, BidType.MANUAL),
                "Saldo tidak mencukupi");

        verify(concurrencyPort, never()).placeBid(any(), anyLong(), anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong());
    }

    @Test
    void placeBid_walletGenericError_throwsGagalMenahanSaldo() {
        doThrow(new RuntimeException("DB down"))
                .when(walletPort).holdFunds(any(), any());

        assertThrows(IllegalStateException.class,
                () -> service.placeBid(listingId, BigDecimal.valueOf(2000), bidderId, BidType.MANUAL));
    }

    // ─── Anti-snipe extension ───────────────────────────────────────────────────

    @Test
    void placeBid_leading_antiSnipeTriggered_publishesTimeExtendedEvent() {
        BigDecimal amount = BigDecimal.valueOf(2000);
        long extendedEndTime = endTimeMillis + 120_000L; // Redis extended by 2 min

        when(concurrencyPort.placeBid(any(), anyLong(), anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong()))
                .thenReturn(ConcurrencyResult.leading(2000L, bidderId.toString(), extendedEndTime, 2000L));
        when(bidRepository.findTopBid(listingId)).thenReturn(Optional.empty());
        when(bidRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.countByListing(listingId)).thenReturn(1);

        service.placeBid(listingId, amount, bidderId, BidType.MANUAL);

        verify(eventPublisher).publishAuctionTimeExtended(any());
    }

    @Test
    void placeBid_leading_noAntiSnipe_noTimeExtendedEvent() {
        BigDecimal amount = BigDecimal.valueOf(2000);

        when(concurrencyPort.placeBid(any(), anyLong(), anyLong(), any(), any(),
                anyLong(), anyLong(), anyLong()))
                .thenReturn(ConcurrencyResult.leading(2000L, bidderId.toString(), endTimeMillis, 2000L));
        when(bidRepository.findTopBid(listingId)).thenReturn(Optional.empty());
        when(bidRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.countByListing(listingId)).thenReturn(1);

        service.placeBid(listingId, amount, bidderId, BidType.MANUAL);

        verify(eventPublisher, never()).publishAuctionTimeExtended(any());
    }

    // ─── closeAuction ──────────────────────────────────────────────────────────

    @Test
    void closeAuction_withWinnerAboveReserve_publishesWonAndNotifiesSSE() {
        BigDecimal finalPrice = BigDecimal.valueOf(2000);
        Bid winnerBid = acceptedBid(bidderId, finalPrice, finalPrice, BidSource.MANUAL);

        when(listingPort.getListingInfo(listingId)).thenReturn(activeListing);
        when(bidRepository.findTopBid(listingId)).thenReturn(Optional.of(winnerBid));
        when(bidRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.closeAuction(listingId);

        ArgumentCaptor<Bid> captor = ArgumentCaptor.forClass(Bid.class);
        verify(bidRepository).save(captor.capture());
        assertEquals(BidStatus.WON, captor.getValue().getStatus());
        verify(eventPublisher).publishAuctionClosed(any());
        verify(auctionNotifier).publishAuctionEnded(eq(listingId), eq(bidderId), eq(finalPrice), eq("WON"));
    }

    @Test
    void closeAuction_withBidBelowReserve_publishesUnsold() {
        BigDecimal lowPrice = BigDecimal.valueOf(100); // below reservePrice=500
        Bid lowBid = acceptedBid(bidderId, lowPrice, lowPrice, BidSource.MANUAL);

        when(listingPort.getListingInfo(listingId)).thenReturn(activeListing);
        when(bidRepository.findTopBid(listingId)).thenReturn(Optional.of(lowBid));
        when(bidRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.closeAuction(listingId);

        ArgumentCaptor<Bid> captor = ArgumentCaptor.forClass(Bid.class);
        verify(bidRepository).save(captor.capture());
        assertEquals(BidStatus.OUTBID, captor.getValue().getStatus());
        verify(auctionNotifier).publishAuctionEnded(eq(listingId), isNull(), isNull(), eq("UNSOLD"));
    }

    @Test
    void closeAuction_noBids_publishesUnsoldWithoutSavingBid() {
        when(listingPort.getListingInfo(listingId)).thenReturn(activeListing);
        when(bidRepository.findTopBid(listingId)).thenReturn(Optional.empty());

        service.closeAuction(listingId);

        verify(bidRepository, never()).save(any());
        verify(auctionNotifier).publishAuctionEnded(eq(listingId), isNull(), isNull(), eq("UNSOLD"));
    }

    @Test
    void closeAuction_listingNotActive_returnsEarlyWithoutPublishing() {
        ListingPort.ListingInfo soldListing = new ListingPort.ListingInfo(
                sellerId, ListingStatus.CLOSED,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(2000), BigDecimal.valueOf(500),
                BigDecimal.ONE, null, LocalDateTime.now().minusHours(1), bidderId);
        when(listingPort.getListingInfo(listingId)).thenReturn(soldListing);

        service.closeAuction(listingId);

        verify(bidRepository, never()).findTopBid(any());
        verify(eventPublisher, never()).publishAuctionClosed(any());
    }

    // ─── Query methods ─────────────────────────────────────────────────────────

    @Test
    void getBidsForListing_returnsAllBidsMappedToResult() {
        Bid b1 = acceptedBid(bidderId, BigDecimal.valueOf(1000), BigDecimal.valueOf(1000), BidSource.MANUAL);
        Bid b2 = acceptedBid(UUID.randomUUID(), BigDecimal.valueOf(900), null, BidSource.MANUAL);
        when(bidRepository.findByListing(listingId)).thenReturn(List.of(b1, b2));

        List<BiddingUseCase.BidResult> results = service.getBidsForListing(listingId);

        assertEquals(2, results.size());
    }

    @Test
    void getMyBids_returnsBidderBidsMappedToResult() {
        Bid b = acceptedBid(bidderId, BigDecimal.valueOf(1000), null, BidSource.MANUAL);
        when(bidRepository.findByBidder(bidderId)).thenReturn(List.of(b));

        List<BiddingUseCase.BidResult> results = service.getMyBids(bidderId);

        assertEquals(1, results.size());
        assertEquals(bidderId, results.get(0).bidderId());
    }
}

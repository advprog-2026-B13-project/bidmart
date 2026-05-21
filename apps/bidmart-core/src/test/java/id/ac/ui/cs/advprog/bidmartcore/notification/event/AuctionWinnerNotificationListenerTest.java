package id.ac.ui.cs.advprog.bidmartcore.notification.event;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionClosedEvent;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.ListingRepository;
import id.ac.ui.cs.advprog.bidmartcore.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionWinnerNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private AuctionWinnerNotificationListener listener;

    private UUID listingId;
    private UUID sellerId;
    private UUID winnerId;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        winnerId = UUID.randomUUID();
    }

    @Test
    void onAuctionClosed_whenWonAndListingExists_shouldCreateNotification() {
        AuctionClosedEvent event = new AuctionClosedEvent(
                listingId, sellerId, winnerId, BigDecimal.valueOf(1500000),
                AuctionClosedEvent.AuctionResult.WON, LocalDateTime.now()
        );

        Listing listing = new Listing();
        listing.setId(listingId);
        listing.setTitle("Gaming Laptop RTX 4060");
        listing.setSellerId(sellerId);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        listener.onAuctionClosed(event);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(1)).createNotification(
                eq(winnerId),
                eq("AUCTION_WON"),
                messageCaptor.capture()
        );

        String message = messageCaptor.getValue();
        assertTrue(message.contains("Gaming Laptop RTX 4060"));
        assertTrue(message.contains("Rp 1.500.000"));
    }

    @Test
    void onAuctionClosed_whenWonAndListingExistsWithLongTitle_shouldTruncateTitle() {
        AuctionClosedEvent event = new AuctionClosedEvent(
                listingId, sellerId, winnerId, BigDecimal.valueOf(1500000),
                AuctionClosedEvent.AuctionResult.WON, LocalDateTime.now()
        );

        Listing listing = new Listing();
        listing.setId(listingId);
        // Title has 60 characters
        listing.setTitle("Super Ultra Rare Vintage Shiny Charizard First Edition Card 1999");
        listing.setSellerId(sellerId);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        listener.onAuctionClosed(event);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(1)).createNotification(
                eq(winnerId),
                eq("AUCTION_WON"),
                messageCaptor.capture()
        );

        String message = messageCaptor.getValue();
        String expectedTitle = "Super Ultra Rare Vintage Shiny Charizard First Edi...";
        assertTrue(message.contains(expectedTitle));
    }

    @Test
    void onAuctionClosed_whenWonAndListingDoesNotExist_shouldDoNothing() {
        AuctionClosedEvent event = new AuctionClosedEvent(
                listingId, sellerId, winnerId, BigDecimal.valueOf(1500000),
                AuctionClosedEvent.AuctionResult.WON, LocalDateTime.now()
        );

        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        listener.onAuctionClosed(event);

        verify(notificationService, never()).createNotification(any(), any(), any());
    }

    @Test
    void onAuctionClosed_whenUnsold_shouldDoNothing() {
        AuctionClosedEvent event = new AuctionClosedEvent(
                listingId, sellerId, null, null,
                AuctionClosedEvent.AuctionResult.UNSOLD, LocalDateTime.now()
        );

        listener.onAuctionClosed(event);

        verifyNoInteractions(listingRepository);
        verifyNoInteractions(notificationService);
    }
}

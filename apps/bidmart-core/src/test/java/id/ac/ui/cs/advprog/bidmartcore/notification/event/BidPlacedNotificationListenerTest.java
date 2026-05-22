package id.ac.ui.cs.advprog.bidmartcore.notification.event;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.BidPlacedEvent;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidPlacedNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private BidPlacedNotificationListener listener;

    private UUID listingId;
    private UUID bidderId;
    private UUID sellerId;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();
        bidderId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
    }

    @Test
    void onBidPlaced_whenListingExists_shouldCreateNotificationForSeller() {
        BidPlacedEvent event = new BidPlacedEvent(
                listingId, UUID.randomUUID(), bidderId, BigDecimal.valueOf(2500000), LocalDateTime.now()
        );

        Listing listing = new Listing();
        listing.setId(listingId);
        listing.setTitle("Sony WH-1000XM5");
        listing.setSellerId(sellerId);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        listener.onBidPlaced(event);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(1)).createNotification(
                eq(sellerId),
                eq("NEW_BID"),
                messageCaptor.capture(),
                eq(listingId)
        );

        String message = messageCaptor.getValue();
        assertTrue(message.contains("Sony WH-1000XM5"));
        assertTrue(message.contains("Rp 2.500.000"));
    }

    @Test
    void onBidPlaced_whenListingDoesNotExist_shouldDoNothing() {
        BidPlacedEvent event = new BidPlacedEvent(
                listingId, UUID.randomUUID(), bidderId, BigDecimal.valueOf(2500000), LocalDateTime.now()
        );

        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        listener.onBidPlaced(event);

        verify(notificationService, never()).createNotification(any(), any(), any(), any());
    }
}
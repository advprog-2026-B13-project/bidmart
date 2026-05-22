package id.ac.ui.cs.advprog.bidmartcore.notification.event;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.OutbidEvent;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutbidListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private OutbidListener listener;

    private UUID listingId;
    private UUID outbidBidderId;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();
        outbidBidderId = UUID.randomUUID();
    }

    @Test
    void onOutbid_shouldCreateNotificationForOutbidBidder() {
        OutbidEvent event = new OutbidEvent(
                listingId, outbidBidderId, BigDecimal.valueOf(100000), BigDecimal.valueOf(110000), LocalDateTime.now(), BigDecimal.valueOf(100000)
        );

        Listing listing = new Listing();
        listing.setId(listingId);
        listing.setTitle("Test Listing");

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        listener.onOutbid(event);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(1)).createNotification(
                eq(outbidBidderId),
                eq("OUTBID"),
                messageCaptor.capture(),
                eq(listingId)
        );

        String message = messageCaptor.getValue();
        assertTrue(message.contains("Rp 110.000"));
        assertTrue(message.contains("You've been outbid on 'Test Listing'"));
    }
}
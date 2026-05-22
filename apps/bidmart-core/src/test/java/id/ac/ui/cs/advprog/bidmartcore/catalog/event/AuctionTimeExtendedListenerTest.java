package id.ac.ui.cs.advprog.bidmartcore.catalog.event;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionTimeExtendedEvent;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.ListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionTimeExtendedListenerTest {

    @Mock
    private ListingService listingService;

    @InjectMocks
    private AuctionTimeExtendedListener auctionTimeExtendedListener;

    @Mock
    private AuctionTimeExtendedEvent auctionTimeExtendedEvent;

    private UUID listingId;
    private LocalDateTime previousEndTime;
    private LocalDateTime newEndTime;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();
        previousEndTime = LocalDateTime.now().plusHours(1);
        newEndTime = previousEndTime.plusMinutes(2);
        when(auctionTimeExtendedEvent.getListingId()).thenReturn(listingId);
    }

    @Test
    @DisplayName("Positive Case: Sukses menangani event perpanjangan waktu dan memperbarui endTime lelang")
    void testOnAuctionTimeExtendedSuccess() {
        when(auctionTimeExtendedEvent.getPreviousEndTime()).thenReturn(previousEndTime);
        when(auctionTimeExtendedEvent.getNewEndTime()).thenReturn(newEndTime);
        doNothing().when(listingService).updateEndTime(listingId, newEndTime);
        auctionTimeExtendedListener.onAuctionTimeExtended(auctionTimeExtendedEvent);
        verify(listingService, times(1)).updateEndTime(listingId, newEndTime);
        verifyNoMoreInteractions(listingService);
    }

    @Test
    @DisplayName("Negative Case: Meneruskan IllegalArgumentException jika target ID produk lelang tidak ditemukan di katalog")
    void testOnAuctionTimeExtendedListingNotFoundThrowsException() {
        when(auctionTimeExtendedEvent.getNewEndTime()).thenReturn(newEndTime);
        doThrow(new IllegalArgumentException("Listing tidak ditemukan"))
                .when(listingService).updateEndTime(listingId, newEndTime);
        assertThrows(IllegalArgumentException.class, () -> {
            auctionTimeExtendedListener.onAuctionTimeExtended(auctionTimeExtendedEvent);
        });
        verify(listingService, times(1)).updateEndTime(listingId, newEndTime);
    }

    @Test
    @DisplayName("Edge Case: Memastikan RuntimeException dari database tetap dilempar ke atas demi menjaga atomisitas rollback transaksi")
    void testOnAuctionTimeExtendedServiceThrowsRuntimeExceptionPropagates() {
        when(auctionTimeExtendedEvent.getNewEndTime()).thenReturn(newEndTime);
        doThrow(new RuntimeException("Database connection timeout"))
                .when(listingService).updateEndTime(listingId, newEndTime);
        assertThrows(RuntimeException.class, () -> {
            auctionTimeExtendedListener.onAuctionTimeExtended(auctionTimeExtendedEvent);
        }, "Listener wajib membiarkan runtime exception lolos demi keselamatan rollback transaksi");

        verify(listingService, times(1)).updateEndTime(listingId, newEndTime);
    }
}
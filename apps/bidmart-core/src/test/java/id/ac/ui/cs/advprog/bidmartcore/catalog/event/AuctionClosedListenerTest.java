package id.ac.ui.cs.advprog.bidmartcore.catalog.event;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionClosedEvent;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.AuctionClosedEvent.AuctionResult;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.ListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionClosedListenerTest {

    @Mock
    private ListingService listingService;

    @InjectMocks
    private AuctionClosedListener auctionClosedListener;

    @Mock
    private AuctionClosedEvent auctionClosedEvent;

    private UUID listingId;
    private UUID winnerId;
    private BigDecimal finalAmount;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();
        winnerId = UUID.randomUUID();
        finalAmount = new BigDecimal("15000000");
        when(auctionClosedEvent.getListingId()).thenReturn(listingId);
    }

    @Test
    @DisplayName("Positive Case: Sukses menangani event WON, merubah status lelang menjadi WON dan mencatat finansial pemenang")
    void testOnAuctionClosedWonSuccess() {
        when(auctionClosedEvent.getResult()).thenReturn(AuctionResult.WON);
        when(auctionClosedEvent.getFinalAmount()).thenReturn(finalAmount);
        when(auctionClosedEvent.getWinnerBidderId()).thenReturn(winnerId);
        auctionClosedListener.onAuctionClosed(auctionClosedEvent);
        verify(listingService, times(1)).updateStatus(listingId, ListingStatus.WON);
        verify(listingService, times(1)).updateCurrentPriceAndWinner(listingId, finalAmount, winnerId);
        verifyNoMoreInteractions(listingService);
    }

    @Test
    @DisplayName("Negative Case: Sukses menangani event UNSOLD, merubah status menjadi UNSOLD tanpa menyentuh data finansial")
    void testOnAuctionClosedUnsoldSuccess() {
        when(auctionClosedEvent.getResult()).thenReturn(AuctionResult.UNSOLD);
        auctionClosedListener.onAuctionClosed(auctionClosedEvent);
        verify(listingService, times(1)).updateStatus(listingId, ListingStatus.UNSOLD);
        verify(listingService, never()).updateCurrentPriceAndWinner(any(), any(), any());
    }

    @Test
    @DisplayName("Edge Case: Memastikan Exception dari layer service diteruskan ke atas demi menjamin atomisitas rollback transaksi")
    void testOnAuctionClosedServiceThrowsExceptionPropagates() {
        when(auctionClosedEvent.getResult()).thenReturn(AuctionResult.WON);
        doThrow(new RuntimeException("Gagal mengunci baris database (Lock Timeout)"))
                .when(listingService).updateStatus(listingId, ListingStatus.WON);
        assertThrows(RuntimeException.class, () -> {
            auctionClosedListener.onAuctionClosed(auctionClosedEvent);
        }, "Listener wajib meneruskan exception ke atas demi keselamatan atomisitas data kelompok");
        verify(listingService, times(1)).updateStatus(listingId, ListingStatus.WON);
        verify(listingService, never()).updateCurrentPriceAndWinner(any(), any(), any());
    }
}
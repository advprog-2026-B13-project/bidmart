package id.ac.ui.cs.advprog.bidmartcore.catalog.event;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.event.BidPlacedEvent;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidPlacedListenerTest {

    @Mock
    private ListingService listingService;

    @InjectMocks
    private BidPlacedListener bidPlacedListener;

    @Mock
    private BidPlacedEvent bidPlacedEvent;

    private UUID listingId;
    private UUID bidderId;
    private BigDecimal bidAmount;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();
        bidderId = UUID.randomUUID();
        bidAmount = new BigDecimal("7500000");
        when(bidPlacedEvent.getListingId()).thenReturn(listingId);
        when(bidPlacedEvent.getBidderId()).thenReturn(bidderId);
        when(bidPlacedEvent.getAmount()).thenReturn(bidAmount);
    }

    @Test
    @DisplayName("Positive Case: Sukses menangani event penawaran baru dan meneruskan data nominal ke ListingService")
    void testOnBidPlacedSuccess() {
        doNothing().when(listingService).updateCurrentPriceAndWinner(listingId, bidAmount, bidderId);
        bidPlacedListener.onBidPlaced(bidPlacedEvent);
        verify(listingService, times(1)).updateCurrentPriceAndWinner(listingId, bidAmount, bidderId);
        verifyNoMoreInteractions(listingService);
    }

    @Test
    @DisplayName("Negative Case: Meneruskan IllegalArgumentException jika target produk lelang ternyata tidak ditemukan")
    void testOnBidPlacedListingNotFoundThrowsException() {
        doThrow(new IllegalArgumentException("Listing tidak ditemukan"))
                .when(listingService).updateCurrentPriceAndWinner(listingId, bidAmount, bidderId);
        assertThrows(IllegalArgumentException.class, () -> {
            bidPlacedListener.onBidPlaced(bidPlacedEvent);
        });

        verify(listingService, times(1)).updateCurrentPriceAndWinner(listingId, bidAmount, bidderId);
    }

    @Test
    @DisplayName("Edge Case: Memastikan RuntimeException dari sistem database merambat naik demi mengamankan atomisitas thread")
    void testOnBidPlacedServiceThrowsRuntimeExceptionPropagates() {
        doThrow(new RuntimeException("Database connection failure"))
                .when(listingService).updateCurrentPriceAndWinner(listingId, bidAmount, bidderId);
        assertThrows(RuntimeException.class, () -> {
            bidPlacedListener.onBidPlaced(bidPlacedEvent);
        }, "Listener wajib meloloskan runtime exception demi keselamatan transaction rollback");

        verify(listingService, times(1)).updateCurrentPriceAndWinner(listingId, bidAmount, bidderId);
    }
}
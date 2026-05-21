package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceImplTest {

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private ListingServiceImpl listingService;

    private UUID listingId;
    private UUID userId;
    private Listing sampleListing;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();
        userId = UUID.randomUUID();

        sampleListing = new Listing();
        sampleListing.setId(listingId);
        sampleListing.setSellerId(userId);
        sampleListing.setTitle("PlayStation 5 Pro");
        sampleListing.setStartingPrice(new BigDecimal("12000000"));
        sampleListing.setCurrentPrice(new BigDecimal("12000000"));
        sampleListing.setBidCount(0);
        sampleListing.setStatus(ListingStatus.ACTIVE);
        sampleListing.setStartTime(LocalDateTime.now().minusDays(1));
        sampleListing.setEndTime(LocalDateTime.now().plusDays(2));
    }

    @Test
    @DisplayName("Positive Case [takeDownListing]: Sukses memaksa status CLOSED dan mencatat audit trail")
    void testTakeDownListingSuccess() {
        String reason = "Pelanggaran TOS Marketplace";
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));
        Listing result = listingService.takeDownListing(listingId, reason, userId);
        assertNotNull(result);
        assertEquals(ListingStatus.CLOSED, result.getStatus(), "Status wajib berubah menjadi CLOSED");
        assertEquals(userId, result.getModeratedByAdminId());
        assertEquals(reason, result.getTakedownReason());
        verify(listingRepository, times(1)).save(sampleListing);
    }

    @Test
    @DisplayName("Negative Case [takeDownListing]: Melempar IllegalArgumentException jika Listing ID tidak valid")
    void testTakeDownListingNotFoundThrowsException() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> {
            listingService.takeDownListing(listingId, "Alasan", userId);
        });
        verify(listingRepository, never()).save(any(Listing.class));
    }

    @Test
    @DisplayName("Edge Case [takeDownListing]: Menolak takedown jika status lelang sudah final (WON/UNSOLD)")
    void testTakeDownListingRejectedOnFinalStatus() {
        sampleListing.setStatus(ListingStatus.WON);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            listingService.takeDownListing(listingId, "Alasan", userId);
        });

        assertTrue(exception.getMessage().contains("tidak dapat di-takedown"));
        verify(listingRepository, never()).save(any(Listing.class));
    }

    @Test
    @DisplayName("Positive Case [updateCurrentPriceAndWinner]: Sukses menaikkan harga lelang dan menambah hitungan bid")
    void testUpdateCurrentPriceAndWinnerSuccess() {
        BigDecimal newBidAmount = new BigDecimal("13500000");
        UUID bidderId = UUID.randomUUID();
        sampleListing.setBidCount(2);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));
        listingService.updateCurrentPriceAndWinner(listingId, newBidAmount, bidderId);
        assertEquals(newBidAmount, sampleListing.getCurrentPrice());
        assertEquals(bidderId, sampleListing.getWinnerId());
        verify(listingRepository, times(1)).save(sampleListing);
    }

    @Test
    @DisplayName("Negative Case [updateCurrentPriceAndWinner]: Gagal update harga jika listing tidak eksis")
    void testUpdateCurrentPriceAndWinnerNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> {
            listingService.updateCurrentPriceAndWinner(listingId, new BigDecimal("13000000"), UUID.randomUUID());
        });
    }

    @Test
    @DisplayName("Edge Case [updateCurrentPriceAndWinner]: Menolak update harga jika nominal bid baru mundur/lebih rendah (Anti Out-of-Order)")
    void testUpdateCurrentPriceAndWinnerRejectedIfPriceGoesBackward() {
        BigDecimal invalidLowBid = new BigDecimal("11000000");
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        assertThrows(IllegalArgumentException.class, () -> {
            listingService.updateCurrentPriceAndWinner(listingId, invalidLowBid, UUID.randomUUID());
        });
        verify(listingRepository, never()).save(any(Listing.class));
    }

    @Test
    @DisplayName("Positive Case [updateStatus]: Sukses memperbarui status dari ACTIVE menjadi CLOSED")
    void testUpdateStatusSuccess() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));
        listingService.updateStatus(listingId, ListingStatus.CLOSED);
        assertEquals(ListingStatus.CLOSED, sampleListing.getStatus());
        verify(listingRepository, times(1)).save(sampleListing);
    }

    @Test
    @DisplayName("Negative Case [updateStatus]: Melempar ralat jika entitas lelang tidak ditemukan")
    void testUpdateStatusNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> {
            listingService.updateStatus(listingId, ListingStatus.WON);
        });
    }

    @Test
    @DisplayName("Edge Case [updateStatus]: Menolak update ulang jika status saat ini sudah final (WON atau UNSOLD)")
    void testUpdateStatusRejectedIfAlreadyFinal() {
        sampleListing.setStatus(ListingStatus.UNSOLD);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        assertThrows(IllegalStateException.class, () -> {
            listingService.updateStatus(listingId, ListingStatus.ACTIVE);
        });
        verify(listingRepository, never()).save(any(Listing.class));
    }

    @Test
    @DisplayName("Positive Case [isListingValidForBid]: Mengembalikan true jika status ACTIVE dan waktu lelang valid")
    void testIsListingValidForBidTrue() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        boolean isValid = listingService.isListingValidForBid(listingId);
        assertTrue(isValid, "Listing harus valid jika berada dalam rentang waktu lelang");
    }

    @Test
    @DisplayName("Negative Case [isListingValidForBid]: Mengembalikan false (Fail-Safe) jika listing tidak ditemukan")
    void testIsListingValidForBidFalseIfNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        boolean isValid = listingService.isListingValidForBid(listingId);
        assertFalse(isValid, "Harus fail-safe mengembalikan false jika ID tidak ada");
    }

    @Test
    @DisplayName("Edge Case [isListingValidForBid]: Mengembalikan false jika status ACTIVE tapi waktu lelang telah kadaluwarsa (Expired)")
    void testIsListingValidForBidFalseIfExpired() {
        sampleListing.setStartTime(LocalDateTime.now().minusDays(5));
        sampleListing.setEndTime(LocalDateTime.now().minusDays(1));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        boolean isValid = listingService.isListingValidForBid(listingId);
        assertFalse(isValid, "Harus mengembalikan false jika waktu penutupan lelang sudah terlampaui");
    }
}
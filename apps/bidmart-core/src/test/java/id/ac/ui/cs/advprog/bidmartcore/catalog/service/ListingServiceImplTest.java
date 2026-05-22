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

    @Test
    @DisplayName("Positive Case [createListing]: Sukses membuat listing baru dalam status DRAFT")
    void testCreateListingSuccess() {
        id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingCreateRequest req = new id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingCreateRequest();
        req.setCategoryId(1);
        req.setStartTime(LocalDateTime.now().plusDays(1));
        req.setEndTime(LocalDateTime.now().plusDays(3));
        req.setTitle("iPhone 15 Pro");

        Category mockCategory = new Category();
        mockCategory.setId(1);

        when(categoryRepository.findById(1)).thenReturn(Optional.of(mockCategory));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        Listing result = listingService.createListing(req, userId);

        assertNotNull(result);
        assertEquals(ListingStatus.DRAFT, result.getStatus(), "Listing baru wajib berstatus DRAFT");
        assertEquals("iPhone 15 Pro", result.getTitle());
    }

    @Test
    @DisplayName("Negative Case [createListing]: Melempar IllegalArgumentException jika kategori tidak ditemukan")
    void testCreateListingCategoryNotFound() {
        id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingCreateRequest req = new id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingCreateRequest();
        req.setCategoryId(404);

        when(categoryRepository.findById(404)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> listingService.createListing(req, userId));
        verify(listingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Edge Case [createListing]: Menolak pembuatan jika waktu selesai mendahului waktu mulai")
    void testCreateListingTimeInvalid() {
        id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingCreateRequest req = new id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingCreateRequest();
        req.setCategoryId(1);
        req.setStartTime(LocalDateTime.now().plusDays(5));
        req.setEndTime(LocalDateTime.now().plusDays(2));

        Category mockCategory = new Category();
        when(categoryRepository.findById(1)).thenReturn(Optional.of(mockCategory));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> listingService.createListing(req, userId));
        assertEquals("Waktu selesai lelang harus setelah waktu mulai", ex.getMessage());
    }

    @Test
    @DisplayName("Positive Case [updateListing]: Pemilik sukses memperbarui detail listing DRAFT yang belum ada bid")
    void testUpdateListingSuccess() {
        id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingUpdateRequest req = new id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingUpdateRequest();
        req.setDescription("Deskripsi Baru");
        req.setStartingPrice(new BigDecimal("15000000"));

        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(0);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        Listing result = listingService.updateListing(listingId, userId, req);

        assertNotNull(result);
        assertEquals("Deskripsi Baru", result.getDescription());
    }

    @Test
    @DisplayName("Negative Case [updateListing]: Melempar ralat jika ID listing tidak ditemukan")
    void testUpdateListingNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> listingService.updateListing(listingId, userId, null));
    }

    @Test
    @DisplayName("Edge Case [updateListing]: Menolak update jika listing sudah memiliki penawaran (bidCount > 0)")
    void testUpdateListingHasBidsConflict() {
        id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingUpdateRequest req = new id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingUpdateRequest();
        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(3); // Ada bid masuk!

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> listingService.updateListing(listingId, userId, req));
        assertTrue(ex.getMessage().contains("sudah memiliki penawaran"));
    }

    @Test
    @DisplayName("Positive Case [getListingForOwner]: Sukses mengambil data jika requester adalah owner asli")
    void testGetListingForOwnerSuccess() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        Listing result = listingService.getListingForOwner(listingId, userId);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Negative Case [getListingForOwner]: Melempar IllegalArgumentException jika listing gaib")
    void testGetListingForOwnerNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> listingService.getListingForOwner(listingId, userId));
    }

    @Test
    @DisplayName("Edge Case [getListingForOwner]: Melempar SecurityException jika diintip oleh user lain")
    void testGetListingForOwnerAccessDenied() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        assertThrows(SecurityException.class, () -> listingService.getListingForOwner(listingId, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Positive Case [activateListing]: Pemilik sukses mengaktifkan listing dari DRAFT ke ACTIVE")
    void testActivateListingSuccess() {
        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(0);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        Listing result = listingService.activateListing(listingId, userId);
        assertEquals(ListingStatus.ACTIVE, result.getStatus());
    }

    @Test
    @DisplayName("Negative Case [activateListing]: Gagal aktivasi jika listing tidak eksis")
    void testActivateListingNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> listingService.activateListing(listingId, userId));
    }

    @Test
    @DisplayName("Edge Case [activateListing]: Menolak aktivasi ganda jika status sudah bukan DRAFT")
    void testActivateListingAlreadyActive() {
        sampleListing.setStatus(ListingStatus.ACTIVE);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertThrows(IllegalStateException.class, () -> listingService.activateListing(listingId, userId));
    }

    @Test
    @DisplayName("Positive Case [closeListing]: Pemilik sukses menutup listing ACTIVE sebelum waktu mulai")
    void testCloseListingSuccess() {
        sampleListing.setStatus(ListingStatus.ACTIVE);
        sampleListing.setStartTime(LocalDateTime.now().plusDays(1)); // Belum mulai

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        Listing result = listingService.closeListing(listingId, userId);
        assertEquals(ListingStatus.CLOSED, result.getStatus());
    }

    @Test
    @DisplayName("Negative Case [closeListing]: Gagal menutup jika entitas tidak terdaftar")
    void testCloseListingNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> listingService.closeListing(listingId, userId));
    }

    @Test
    @DisplayName("Edge Case [closeListing]: Menolak penutupan paksa jika waktu lelang sudah terlanjur berjalan")
    void testCloseListingAlreadyStarted() {
        sampleListing.setStatus(ListingStatus.ACTIVE);
        sampleListing.setStartTime(LocalDateTime.now().minusHours(1)); // Sudah mulai!

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertThrows(IllegalStateException.class, () -> listingService.closeListing(listingId, userId));
    }

    @Test
    @DisplayName("Positive Case [deleteListing]: Sukses menghapus listing DRAFT yang bersih dari penawaran")
    void testDeleteListingSuccess() {
        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(0);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        doNothing().when(listingRepository).deleteById(listingId);

        assertDoesNotThrow(() -> listingService.deleteListing(listingId, userId));
        verify(listingRepository, times(1)).deleteById(listingId);
    }

    @Test
    @DisplayName("Negative Case [deleteListing]: Gagal hapus jika ID listing salah")
    void testDeleteListingNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> listingService.deleteListing(listingId, userId));
    }

    @Test
    @DisplayName("Edge Case [deleteListing]: Menolak penghapusan jika status sudah bukan DRAFT")
    void testDeleteListingInvalidStatus() {
        sampleListing.setStatus(ListingStatus.ACTIVE);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertThrows(IllegalStateException.class, () -> listingService.deleteListing(listingId, userId));
    }

    @Test
    @DisplayName("Positive Case [updateEndTime]: Sukses memperbarui batas waktu penutupan lelang")
    void testUpdateEndTimeSuccess() {
        LocalDateTime nextWeek = LocalDateTime.now().plusDays(7);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> listingService.updateEndTime(listingId, nextWeek));
        assertEquals(nextWeek, sampleListing.getEndTime());
    }

    @Test
    @DisplayName("Negative Case [updateEndTime]: Melempar exception jika ID produk salah")
    void testUpdateEndTimeNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> listingService.updateEndTime(listingId, LocalDateTime.now()));
    }

    @Test
    @DisplayName("Edge Case [updateFinalResult]: Sukses menyimpan harga final pemenang tanpa merusak data lain")
    void testUpdateFinalResultSuccess() {
        BigDecimal finalPrice = new BigDecimal("50000000");
        UUID winner = UUID.randomUUID();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        listingService.updateFinalResult(listingId, finalPrice, winner);

        assertEquals(finalPrice, sampleListing.getCurrentPrice());
        assertEquals(winner, sampleListing.getWinnerId());
    }

    @Test
    @DisplayName("Positive Case [getListingById]: Sukses menemukan dan mengambil detail produk lelang berdasarkan ID yang valid")
    void testGetListingByIdSuccess() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        Listing result = listingService.getListingById(listingId);
        assertNotNull(result);
        assertEquals(listingId, result.getId());
        assertEquals("PlayStation 5 Pro", result.getTitle());
        verify(listingRepository, times(1)).findById(listingId);
    }

    @Test
    @DisplayName("Negative Case [getListingById]: Melempar IllegalArgumentException dengan pesan khusus jika ID tidak terdaftar")
    void testGetListingByIdNotFoundThrowsException() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            listingService.getListingById(listingId);
        });

        assertEquals("Listing dengan ID tersebut tidak ditemukan", exception.getMessage());
        verify(listingRepository, times(1)).findById(listingId);
    }

    @Test
    @DisplayName("Edge Case [getListingById]: Menangani request pencarian dengan parameter ID bernilai null secara aman")
    void testGetListingByIdWithNullIdThrowsException() {
        when(listingRepository.findById(null)).thenReturn(Optional.empty());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            listingService.getListingById(null);
        });

        assertEquals("Listing dengan ID tersebut tidak ditemukan", exception.getMessage());
        verify(listingRepository, times(1)).findById(null);
    }

    @Test
    @DisplayName("Positive Case [searchListings]: Sukses mencari produk lelang menggunakan kombinasi kata kunci dan batas harga")
    void testSearchListingsSuccess() {
        // Arrange
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Listing> expectedPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(sampleListing));

        when(listingRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(expectedPage);
        org.springframework.data.domain.Page<Listing> result = listingService.searchListings(
                "PlayStation", new BigDecimal("1000000"), new BigDecimal("20000000"), null, pageable);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("PlayStation 5 Pro", result.getContent().get(0).getTitle());
        verify(listingRepository, times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Negative Case [searchListings]: Sukses memulangkan halaman (Page) kosong jika tidak ada produk yang cocok dengan kriteria")
    void testSearchListingsEmptyResult() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(listingRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        org.springframework.data.domain.Page<Listing> result = listingService.searchListings(
                "BarangAnehGaibPastiKosong", null, null, null, pageable);
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Harus memulangkan kontainer Page kosong, bukan bernilai null");
        verify(listingRepository, times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Edge Case [searchListings]: Sukses mengeksekusi pencarian rekursif hierarki sub-kategori saat parameter categoryId disuplai")
    void testSearchListingsWithCategoryIdRecursionSuccess() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        Integer inputCategoryId = 1;
        when(categoryRepository.findByParentCategoryId(inputCategoryId)).thenReturn(java.util.List.of());
        when(listingRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(org.springframework.data.domain.Page.empty());
        org.springframework.data.domain.Page<Listing> result = listingService.searchListings(
                null, null, null, inputCategoryId, pageable);
        assertNotNull(result);
        verify(categoryRepository, times(1)).findByParentCategoryId(inputCategoryId);
        verify(listingRepository, times(1)).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }
}
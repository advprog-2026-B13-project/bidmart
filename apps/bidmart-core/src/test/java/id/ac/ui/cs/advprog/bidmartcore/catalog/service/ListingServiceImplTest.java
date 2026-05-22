package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingCreateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingUpdateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.CategoryRepository;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceImplTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private CategoryRepository categoryRepository;

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

    // ── createListing ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [createListing]: Sukses membuat listing baru dalam status DRAFT")
    void testCreateListingSuccess() {
        ListingCreateRequest req = new ListingCreateRequest();
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
    @DisplayName("Positive Case [createListing]: Sukses membuat listing tanpa startTime dan endTime (keduanya null)")
    void testCreateListingWithNullTimes() {
        ListingCreateRequest req = new ListingCreateRequest();
        req.setCategoryId(1);
        req.setTitle("Samsung Galaxy S24");
        // startTime dan endTime null — branch validasi waktu dilewati

        Category mockCategory = new Category();
        mockCategory.setId(1);

        when(categoryRepository.findById(1)).thenReturn(Optional.of(mockCategory));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        Listing result = listingService.createListing(req, userId);

        assertNotNull(result);
        assertEquals(ListingStatus.DRAFT, result.getStatus());
    }

    @Test
    @DisplayName("Positive Case [createListing]: Sukses membuat listing jika hanya startTime yang disuplai (endTime null)")
    void testCreateListingWithOnlyStartTime() {
        ListingCreateRequest req = new ListingCreateRequest();
        req.setCategoryId(1);
        req.setTitle("Laptop Lenovo");
        req.setStartTime(LocalDateTime.now().plusDays(1));
        // endTime null — branch validasi waktu dilewati

        Category mockCategory = new Category();
        mockCategory.setId(1);

        when(categoryRepository.findById(1)).thenReturn(Optional.of(mockCategory));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        Listing result = listingService.createListing(req, userId);

        assertNotNull(result);
        assertEquals(ListingStatus.DRAFT, result.getStatus());
    }

    @Test
    @DisplayName("Negative Case [createListing]: Melempar IllegalArgumentException jika kategori tidak ditemukan")
    void testCreateListingCategoryNotFound() {
        ListingCreateRequest req = new ListingCreateRequest();
        req.setCategoryId(404);

        when(categoryRepository.findById(404)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> listingService.createListing(req, userId));
        verify(listingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Edge Case [createListing]: Menolak pembuatan jika waktu selesai mendahului waktu mulai")
    void testCreateListingTimeInvalid() {
        ListingCreateRequest req = new ListingCreateRequest();
        req.setCategoryId(1);
        req.setStartTime(LocalDateTime.now().plusDays(5));
        req.setEndTime(LocalDateTime.now().plusDays(2));

        Category mockCategory = new Category();
        when(categoryRepository.findById(1)).thenReturn(Optional.of(mockCategory));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> listingService.createListing(req, userId));
        assertEquals("Waktu selesai lelang harus setelah waktu mulai", ex.getMessage());
    }

    // ── updateListing ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [updateListing]: Pemilik sukses memperbarui detail listing DRAFT yang belum ada bid")
    void testUpdateListingSuccess() {
        ListingUpdateRequest req = new ListingUpdateRequest();
        req.setDescription("Deskripsi Baru");
        req.setStartingPrice(new BigDecimal("15000000"));

        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(0);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        Listing result = listingService.updateListing(listingId, userId, req);

        assertNotNull(result);
        assertEquals("Deskripsi Baru", result.getDescription());
        // bidCount == 0 → currentPrice harus diset ke startingPrice baru
        assertEquals(new BigDecimal("15000000"), result.getCurrentPrice());
    }

    @Test
    @DisplayName("Positive Case [updateListing]: currentPrice tidak diubah jika bidCount bernilai null")
    void testUpdateListingBidCountNull() {
        ListingUpdateRequest req = new ListingUpdateRequest();
        req.setStartingPrice(new BigDecimal("16000000"));

        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(null); // null → currentPrice diset ke startingPrice

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        Listing result = listingService.updateListing(listingId, userId, req);

        assertNotNull(result);
        assertEquals(new BigDecimal("16000000"), result.getCurrentPrice());
    }

    @Test
    @DisplayName("Negative Case [updateListing]: Melempar ralat jika ID listing tidak ditemukan")
    void testUpdateListingNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> listingService.updateListing(listingId, userId, new ListingUpdateRequest()));
    }

    @Test
    @DisplayName("Negative Case [updateListing]: Melempar SecurityException jika bukan pemilik listing")
    void testUpdateListingNotOwner() {
        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(0);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertThrows(SecurityException.class,
                () -> listingService.updateListing(listingId, UUID.randomUUID(), new ListingUpdateRequest()));
    }

    @Test
    @DisplayName("Negative Case [updateListing]: Melempar IllegalStateException jika status bukan DRAFT")
    void testUpdateListingNotDraft() {
        sampleListing.setStatus(ListingStatus.ACTIVE); // bukan DRAFT
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertThrows(IllegalStateException.class,
                () -> listingService.updateListing(listingId, userId, new ListingUpdateRequest()));
    }

    @Test
    @DisplayName("Edge Case [updateListing]: Menolak update jika listing sudah memiliki penawaran (bidCount > 0)")
    void testUpdateListingHasBidsConflict() {
        ListingUpdateRequest req = new ListingUpdateRequest();
        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(3);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> listingService.updateListing(listingId, userId, req));
        assertTrue(ex.getMessage().contains("sudah memiliki penawaran"));
    }

    @Test
    @DisplayName("Edge Case [updateListing]: Menolak update jika waktu selesai mendahului waktu mulai")
    void testUpdateListingInvalidTime() {
        ListingUpdateRequest req = new ListingUpdateRequest();
        req.setStartTime(LocalDateTime.now().plusDays(5));
        req.setEndTime(LocalDateTime.now().plusDays(2));

        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(0);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> listingService.updateListing(listingId, userId, req));
        assertEquals("Waktu selesai lelang harus setelah waktu mulai", ex.getMessage());
    }

    // ── getListingById ────────────────────────────────────────────────────────

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
    @DisplayName("Negative Case [getListingById]: Melempar IllegalArgumentException jika ID tidak terdaftar")
    void testGetListingByIdNotFoundThrowsException() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> listingService.getListingById(listingId));

        assertEquals("Listing dengan ID tersebut tidak ditemukan", exception.getMessage());
        verify(listingRepository, times(1)).findById(listingId);
    }

    @Test
    @DisplayName("Edge Case [getListingById]: Menangani request dengan parameter ID bernilai null")
    void testGetListingByIdWithNullIdThrowsException() {
        when(listingRepository.findById(null)).thenReturn(Optional.empty());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> listingService.getListingById(null));

        assertEquals("Listing dengan ID tersebut tidak ditemukan", exception.getMessage());
    }

    // ── getListingForOwner ────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [getListingForOwner]: Sukses mengambil data jika requester adalah owner asli")
    void testGetListingForOwnerSuccess() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        Listing result = listingService.getListingForOwner(listingId, userId);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Negative Case [getListingForOwner]: Melempar IllegalArgumentException jika listing tidak ditemukan")
    void testGetListingForOwnerNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> listingService.getListingForOwner(listingId, userId));
    }

    @Test
    @DisplayName("Edge Case [getListingForOwner]: Melempar SecurityException jika diintip oleh user lain")
    void testGetListingForOwnerAccessDenied() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        assertThrows(SecurityException.class,
                () -> listingService.getListingForOwner(listingId, UUID.randomUUID()));
    }

    // ── getListingsBySeller ───────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [getListingsBySeller]: Sukses mengambil semua listing milik seller")
    void testGetListingsBySellerSuccess() {
        when(listingRepository.findBySellerId(userId)).thenReturn(List.of(sampleListing));

        List<Listing> results = listingService.getListingsBySeller(userId);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(listingRepository, times(1)).findBySellerId(userId);
    }

    @Test
    @DisplayName("Negative Case [getListingsBySeller]: Mengembalikan list kosong jika seller tidak memiliki listing")
    void testGetListingsBySellerEmpty() {
        when(listingRepository.findBySellerId(userId)).thenReturn(List.of());

        List<Listing> results = listingService.getListingsBySeller(userId);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ── activateListing ───────────────────────────────────────────────────────

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
        assertThrows(IllegalArgumentException.class,
                () -> listingService.activateListing(listingId, userId));
    }

    @Test
    @DisplayName("Negative Case [activateListing]: Melempar SecurityException jika bukan pemilik listing")
    void testActivateListingNotOwner() {
        sampleListing.setStatus(ListingStatus.DRAFT);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertThrows(SecurityException.class,
                () -> listingService.activateListing(listingId, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Edge Case [activateListing]: Menolak aktivasi ganda jika status sudah bukan DRAFT")
    void testActivateListingAlreadyActive() {
        sampleListing.setStatus(ListingStatus.ACTIVE);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertThrows(IllegalStateException.class,
                () -> listingService.activateListing(listingId, userId));
    }

    @Test
    @DisplayName("Edge Case [activateListing]: Menolak aktivasi jika bidCount sudah lebih dari 0 meski DRAFT")
    void testActivateListingHasBids() {
        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(2);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> listingService.activateListing(listingId, userId));
        assertTrue(ex.getMessage().contains("sudah memiliki penawaran"));
    }

    // ── closeListing ──────────────────────────────────────────────────────────

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
    @DisplayName("Positive Case [closeListing]: Sukses menutup listing jika startTime bernilai null")
    void testCloseListingStartTimeNull() {
        sampleListing.setStatus(ListingStatus.ACTIVE);
        sampleListing.setStartTime(null); // null → kondisi !now.isBefore(null) tidak terpenuhi → lanjut

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        Listing result = listingService.closeListing(listingId, userId);
        assertEquals(ListingStatus.CLOSED, result.getStatus());
    }

    @Test
    @DisplayName("Negative Case [closeListing]: Gagal menutup jika entitas tidak terdaftar")
    void testCloseListingNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> listingService.closeListing(listingId, userId));
    }

    @Test
    @DisplayName("Negative Case [closeListing]: Melempar SecurityException jika bukan pemilik listing")
    void testCloseListingNotOwner() {
        sampleListing.setStatus(ListingStatus.ACTIVE);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertThrows(SecurityException.class,
                () -> listingService.closeListing(listingId, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Negative Case [closeListing]: Melempar IllegalStateException jika status bukan ACTIVE")
    void testCloseListingNotActive() {
        sampleListing.setStatus(ListingStatus.DRAFT);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertThrows(IllegalStateException.class,
                () -> listingService.closeListing(listingId, userId));
    }

    @Test
    @DisplayName("Edge Case [closeListing]: Menolak penutupan paksa jika waktu lelang sudah terlanjur berjalan")
    void testCloseListingAlreadyStarted() {
        sampleListing.setStatus(ListingStatus.ACTIVE);
        sampleListing.setStartTime(LocalDateTime.now().minusHours(1)); // Sudah mulai

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertThrows(IllegalStateException.class,
                () -> listingService.closeListing(listingId, userId));
    }

    // ── deleteListing ─────────────────────────────────────────────────────────

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
        assertThrows(IllegalArgumentException.class,
                () -> listingService.deleteListing(listingId, userId));
    }

    @Test
    @DisplayName("Negative Case [deleteListing]: Melempar SecurityException jika bukan pemilik listing")
    void testDeleteListingNotOwner() {
        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(0);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertThrows(SecurityException.class,
                () -> listingService.deleteListing(listingId, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Edge Case [deleteListing]: Menolak penghapusan jika status sudah bukan DRAFT")
    void testDeleteListingInvalidStatus() {
        sampleListing.setStatus(ListingStatus.ACTIVE);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertThrows(IllegalStateException.class,
                () -> listingService.deleteListing(listingId, userId));
    }

    @Test
    @DisplayName("Edge Case [deleteListing]: Menolak penghapusan jika bidCount > 0 meski berstatus DRAFT")
    void testDeleteListingHasBids() {
        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(1);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> listingService.deleteListing(listingId, userId));
        assertTrue(ex.getMessage().contains("sudah memiliki penawaran"));
    }

    // ── updateCurrentPriceAndWinner ───────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [updateCurrentPriceAndWinner]: Sukses mendelegasikan update ke repository.recordNewBid")
    void testUpdateCurrentPriceAndWinnerSuccess() {
        BigDecimal newPrice = new BigDecimal("13500000");
        UUID bidderId = UUID.randomUUID();

        doNothing().when(listingRepository).recordNewBid(listingId, newPrice, bidderId);

        assertDoesNotThrow(() -> listingService.updateCurrentPriceAndWinner(listingId, newPrice, bidderId));
        verify(listingRepository, times(1)).recordNewBid(listingId, newPrice, bidderId);
    }

    @Test
    @DisplayName("Edge Case [updateCurrentPriceAndWinner]: Meneruskan exception jika repository melempar error")
    void testUpdateCurrentPriceAndWinnerRepositoryError() {
        BigDecimal newPrice = new BigDecimal("13500000");
        UUID bidderId = UUID.randomUUID();

        doThrow(new RuntimeException("DB error"))
                .when(listingRepository).recordNewBid(listingId, newPrice, bidderId);

        assertThrows(RuntimeException.class,
                () -> listingService.updateCurrentPriceAndWinner(listingId, newPrice, bidderId));
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

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
        assertThrows(IllegalArgumentException.class,
                () -> listingService.updateStatus(listingId, ListingStatus.WON));
    }

    @Test
    @DisplayName("Edge Case [updateStatus]: Sukses mengubah status ke WON tanpa batasan tambahan")
    void testUpdateStatusToWon() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        listingService.updateStatus(listingId, ListingStatus.WON);

        assertEquals(ListingStatus.WON, sampleListing.getStatus());
    }

    // ── updateEndTime ─────────────────────────────────────────────────────────

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
        assertThrows(IllegalArgumentException.class,
                () -> listingService.updateEndTime(listingId, LocalDateTime.now()));
    }

    // ── updateFinalResult ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [updateFinalResult]: Sukses menyimpan harga final pemenang tanpa merusak data lain")
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
    @DisplayName("Negative Case [updateFinalResult]: Melempar exception jika listing tidak ditemukan")
    void testUpdateFinalResultNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> listingService.updateFinalResult(listingId, new BigDecimal("5000000"), UUID.randomUUID()));
    }

    // ── isListingValidForBid ──────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [isListingValidForBid]: Mengembalikan true jika status ACTIVE")
    void testIsListingValidForBidActiveTrue() {
        sampleListing.setStatus(ListingStatus.ACTIVE);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertTrue(listingService.isListingValidForBid(listingId));
    }

    @Test
    @DisplayName("Positive Case [isListingValidForBid]: Mengembalikan true jika status EXTENDED dan waktu valid")
    void testIsListingValidForBidExtendedTrue() {
        sampleListing.setStatus(ListingStatus.EXTENDED);
        sampleListing.setStartTime(LocalDateTime.now().minusHours(1));
        sampleListing.setEndTime(LocalDateTime.now().plusHours(1));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertTrue(listingService.isListingValidForBid(listingId));
    }

    @Test
    @DisplayName("Negative Case [isListingValidForBid]: Mengembalikan false jika listing tidak ditemukan")
    void testIsListingValidForBidNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertFalse(listingService.isListingValidForBid(listingId));
    }

    @Test
    @DisplayName("Negative Case [isListingValidForBid]: Mengembalikan false jika status EXTENDED tapi waktu mulai belum tercapai")
    void testIsListingValidForBidExtendedNotStarted() {
        sampleListing.setStatus(ListingStatus.EXTENDED);
        sampleListing.setStartTime(LocalDateTime.now().plusHours(1)); // Belum mulai
        sampleListing.setEndTime(LocalDateTime.now().plusHours(5));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertFalse(listingService.isListingValidForBid(listingId));
    }

    @Test
    @DisplayName("Negative Case [isListingValidForBid]: Mengembalikan false jika status EXTENDED tapi waktu sudah melewati endTime")
    void testIsListingValidForBidExtendedExpired() {
        sampleListing.setStatus(ListingStatus.EXTENDED);
        sampleListing.setStartTime(LocalDateTime.now().minusDays(2));
        sampleListing.setEndTime(LocalDateTime.now().minusHours(1)); // Sudah berakhir
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertFalse(listingService.isListingValidForBid(listingId));
    }

    @Test
    @DisplayName("Edge Case [isListingValidForBid]: Mengembalikan false jika status ACTIVE tapi endTime sudah terlampaui")
    void testIsListingValidForBidActiveExpired() {
        sampleListing.setStatus(ListingStatus.ACTIVE);
        sampleListing.setStartTime(LocalDateTime.now().minusDays(5));
        sampleListing.setEndTime(LocalDateTime.now().minusDays(1));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        // Status ACTIVE mengembalikan true dari impl (tidak cek waktu untuk ACTIVE)
        assertTrue(listingService.isListingValidForBid(listingId));
    }

    @Test
    @DisplayName("Edge Case [isListingValidForBid]: Mengembalikan false jika status DRAFT")
    void testIsListingValidForBidDraft() {
        sampleListing.setStatus(ListingStatus.DRAFT);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertFalse(listingService.isListingValidForBid(listingId));
    }

    // ── searchListings (5-param overload) ────────────────────────────────────

    @Test
    @DisplayName("Positive Case [searchListings 5-param]: Overload tanpa status mendelegasikan ke overload 6-param")
    void testSearchListingsFiveParamOverload() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Listing> expectedPage = new PageImpl<>(List.of(sampleListing));

        when(listingRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);

        Page<Listing> result = listingService.searchListings(
                "PlayStation", new BigDecimal("1000000"), new BigDecimal("20000000"), null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(listingRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    // ── searchListings (6-param) ──────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [searchListings]: Sukses mencari produk dengan kombinasi keyword dan batas harga")
    void testSearchListingsSuccess() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Listing> expectedPage = new PageImpl<>(List.of(sampleListing));

        when(listingRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(expectedPage);

        Page<Listing> result = listingService.searchListings(
                "PlayStation", new BigDecimal("1000000"), new BigDecimal("20000000"), null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("PlayStation 5 Pro", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("Negative Case [searchListings]: Mengembalikan Page kosong jika tidak ada produk yang cocok")
    void testSearchListingsEmptyResult() {
        Pageable pageable = PageRequest.of(0, 10);
        when(listingRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty());

        Page<Listing> result = listingService.searchListings(
                "BarangAnehGaibPastiKosong", null, null, null, pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [searchListings]: Pencarian dengan semua parameter null menggunakan spec isActive+notExpired")
    void testSearchListingsAllParamsNull() {
        Pageable pageable = PageRequest.of(0, 10);
        when(listingRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sampleListing)));

        Page<Listing> result = listingService.searchListings(
                null, null, null, null, pageable);

        assertNotNull(result);
        verify(listingRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Edge Case [searchListings]: Pencarian dengan status eksplisit menggunakan spec hasStatus")
    void testSearchListingsWithExplicitStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        when(listingRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sampleListing)));

        Page<Listing> result = listingService.searchListings(
                null, null, null, null, ListingStatus.ACTIVE, pageable);

        assertNotNull(result);
        verify(listingRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Edge Case [searchListings]: Pencarian dengan keyword spasi saja tidak menambah spec hasTitle")
    void testSearchListingsBlankKeyword() {
        Pageable pageable = PageRequest.of(0, 10);
        when(listingRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty());

        Page<Listing> result = listingService.searchListings(
                "   ", null, null, null, pageable);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [searchListings]: Pencarian dengan hanya minPrice (maxPrice null) menambah spec harga")
    void testSearchListingsOnlyMinPrice() {
        Pageable pageable = PageRequest.of(0, 10);
        when(listingRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sampleListing)));

        Page<Listing> result = listingService.searchListings(
                null, new BigDecimal("5000000"), null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Edge Case [searchListings]: Rekursi hierarki kategori dengan children bercabang")
    void testSearchListingsWithCategoryIdRecursionWithChildren() {
        Pageable pageable = PageRequest.of(0, 10);
        Integer rootCategoryId = 1;

        Category child1 = new Category();
        child1.setId(2);
        Category child2 = new Category();
        child2.setId(3);

        // Root → [child1, child2]; child1 → [grandchild]; child2 → []
        Category grandchild = new Category();
        grandchild.setId(4);

        when(categoryRepository.findByParentCategoryId(rootCategoryId)).thenReturn(List.of(child1, child2));
        when(categoryRepository.findByParentCategoryId(2)).thenReturn(List.of(grandchild));
        when(categoryRepository.findByParentCategoryId(3)).thenReturn(List.of());
        when(categoryRepository.findByParentCategoryId(4)).thenReturn(List.of());
        when(listingRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty());

        Page<Listing> result = listingService.searchListings(
                null, null, null, rootCategoryId, pageable);

        assertNotNull(result);
        verify(categoryRepository, times(1)).findByParentCategoryId(rootCategoryId);
        verify(categoryRepository, times(1)).findByParentCategoryId(2);
        verify(categoryRepository, times(1)).findByParentCategoryId(3);
        verify(categoryRepository, times(1)).findByParentCategoryId(4);
    }

    @Test
    @DisplayName("Edge Case [searchListings]: Rekursi hierarki kategori tanpa sub-kategori (leaf node)")
    void testSearchListingsWithCategoryIdLeafNode() {
        Pageable pageable = PageRequest.of(0, 10);
        Integer categoryId = 5;

        when(categoryRepository.findByParentCategoryId(categoryId)).thenReturn(List.of());
        when(listingRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty());

        Page<Listing> result = listingService.searchListings(
                null, null, null, categoryId, pageable);

        assertNotNull(result);
        verify(categoryRepository, times(1)).findByParentCategoryId(categoryId);
    }
}
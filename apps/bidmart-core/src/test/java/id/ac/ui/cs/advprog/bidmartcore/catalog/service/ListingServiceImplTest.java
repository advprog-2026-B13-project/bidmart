package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
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

    @Mock
    private AuthContext authContext;

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

    // ── createListing (MENGGUNAKAN UUID / userId) ─────────────────────────────

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

    // ── updateListing (MENGGUNAKAN AuthContext) ───────────────────────────────

    @Test
    @DisplayName("Positive Case [updateListing]: Pemilik sukses memperbarui detail listing DRAFT yang belum ada bid")
    void testUpdateListingSuccess() {
        ListingUpdateRequest req = new ListingUpdateRequest();
        req.setDescription("Deskripsi Baru");
        req.setStartingPrice(new BigDecimal("15000000"));

        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(0);

        when(authContext.getUserId()).thenReturn(userId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        Listing result = listingService.updateListing(listingId, authContext, req);

        assertNotNull(result);
        assertEquals("Deskripsi Baru", result.getDescription());
        assertEquals(new BigDecimal("15000000"), result.getCurrentPrice());
    }

    @Test
    @DisplayName("Negative Case [updateListing]: Melempar ralat jika ID listing tidak ditemukan")
    void testUpdateListingNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> listingService.updateListing(listingId, authContext, new ListingUpdateRequest()));
    }

    @Test
    @DisplayName("Negative Case [updateListing]: Melempar SecurityException jika bukan pemilik listing")
    void testUpdateListingNotOwner() {
        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(0);
        when(authContext.getUserId()).thenReturn(UUID.randomUUID());
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertThrows(SecurityException.class,
                () -> listingService.updateListing(listingId, authContext, new ListingUpdateRequest()));
    }

    @Test
    @DisplayName("Negative Case [updateListing]: Melempar IllegalStateException jika status bukan DRAFT")
    void testUpdateListingNotDraft() {
        sampleListing.setStatus(ListingStatus.ACTIVE);
        when(authContext.getUserId()).thenReturn(userId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertThrows(IllegalStateException.class,
                () -> listingService.updateListing(listingId, authContext, new ListingUpdateRequest()));
    }

    @Test
    @DisplayName("Edge Case [updateListing]: Menolak update jika listing sudah memiliki penawaran (bidCount > 0)")
    void testUpdateListingHasBidsConflict() {
        ListingUpdateRequest req = new ListingUpdateRequest();
        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(3);

        when(authContext.getUserId()).thenReturn(userId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> listingService.updateListing(listingId, authContext, req));
        assertTrue(ex.getMessage().contains("sudah memiliki penawaran"));
    }

    // ── getListingById ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [getListingById]: Sukses menemukan dan mengambil detail produk lelang")
    void testGetListingByIdSuccess() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        Listing result = listingService.getListingById(listingId);
        assertNotNull(result);
        assertEquals(listingId, result.getId());
        assertEquals("PlayStation 5 Pro", result.getTitle());
    }

    @Test
    @DisplayName("Negative Case [getListingById]: Melempar IllegalArgumentException jika ID tidak terdaftar")
    void testGetListingByIdNotFoundThrowsException() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> listingService.getListingById(listingId));

        assertEquals("Listing dengan ID tersebut tidak ditemukan", exception.getMessage());
    }

    // ── getListingForOwner (MENGGUNAKAN UUID / userId) ────────────────────────

    @Test
    @DisplayName("Positive Case [getListingForOwner]: Sukses mengambil data jika requester adalah owner asli")
    void testGetListingForOwnerSuccess() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        Listing result = listingService.getListingForOwner(listingId, userId);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Edge Case [getListingForOwner]: Melempar SecurityException jika diintip oleh user lain")
    void testGetListingForOwnerAccessDenied() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        assertThrows(SecurityException.class,
                () -> listingService.getListingForOwner(listingId, UUID.randomUUID()));
    }

    // ── getListingsBySeller (MENGGUNAKAN UUID / userId) ───────────────────────

    @Test
    @DisplayName("Positive Case [getListingsBySeller]: Sukses mengambil semua listing milik seller")
    void testGetListingsBySellerSuccess() {
        when(listingRepository.findBySellerId(userId)).thenReturn(List.of(sampleListing));
        List<Listing> results = listingService.getListingsBySeller(userId);
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    // ── activateListing (MENGGUNAKAN UUID / userId) ───────────────────────────

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

    // ── closeListing (MENGGUNAKAN UUID / userId) ──────────────────────────────

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
    @DisplayName("Negative Case [closeListing]: Melempar IllegalStateException jika status bukan ACTIVE")
    void testCloseListingNotActive() {
        sampleListing.setStatus(ListingStatus.DRAFT);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        assertThrows(IllegalStateException.class,
                () -> listingService.closeListing(listingId, userId));
    }

    // ── deleteListing (MENGGUNAKAN UUID / userId) ─────────────────────────────

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
    @DisplayName("Edge Case [deleteListing]: Menolak penghapusan jika bidCount > 0 meski berstatus DRAFT")
    void testDeleteListingHasBids() {
        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(1);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> listingService.deleteListing(listingId, userId));
        assertTrue(ex.getMessage().contains("sudah memiliki penawaran"));
    }

    // ── updateFinalResult ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [updateFinalResult]: Sukses menyimpan harga final pemenang")
    void testUpdateFinalResultSuccess() {
        BigDecimal finalPrice = new BigDecimal("50000000");
        UUID winner = UUID.randomUUID();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(sampleListing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        listingService.updateFinalResult(listingId, finalPrice, winner);

        assertEquals(finalPrice, sampleListing.getCurrentPrice());
        assertEquals(winner, sampleListing.getWinnerId());
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
    @DisplayName("Negative Case [isListingValidForBid]: Mengembalikan false jika status DRAFT")
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
    }

    // ── canEditListing (MENGGUNAKAN AuthContext) ──────────────────────────────

    @Test
    @DisplayName("Positive Case [canEditListing]: Boleh edit jika user adalah pemegang role super admin")
    void testCanEditListingSuperAdmin() {
        when(authContext.getUserId()).thenReturn(UUID.randomUUID());
        when(authContext.hasPermission(PermissionValue.LISTING_UPDATE_ALL_LISTING)).thenReturn(true);

        assertTrue(listingService.canEditListing(sampleListing, authContext));
    }

    @Test
    @DisplayName("Positive Case [canEditListing]: Boleh edit jika user adalah pemilik listing berstatus DRAFT tanpa bid")
    void testCanEditListingOwnerDraftNoBid() {
        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(0);

        when(authContext.getUserId()).thenReturn(userId);
        when(authContext.hasPermission(PermissionValue.LISTING_UPDATE_ALL_LISTING)).thenReturn(false);

        assertTrue(listingService.canEditListing(sampleListing, authContext));
    }

    @Test
    @DisplayName("Negative Case [canEditListing]: Tidak boleh edit jika listing status ACTIVE")
    void testCanEditListingNotDraft() {
        sampleListing.setStatus(ListingStatus.ACTIVE);

        when(authContext.getUserId()).thenReturn(userId);
        when(authContext.hasPermission(PermissionValue.LISTING_UPDATE_ALL_LISTING)).thenReturn(false);

        assertFalse(listingService.canEditListing(sampleListing, authContext));
    }

    @Test
    @DisplayName("Negative Case [canEditListing]: Tidak boleh edit jika listing sudah memiliki bid")
    void testCanEditListingHasBid() {
        sampleListing.setStatus(ListingStatus.DRAFT);
        sampleListing.setBidCount(1);

        when(authContext.getUserId()).thenReturn(userId);
        when(authContext.hasPermission(PermissionValue.LISTING_UPDATE_ALL_LISTING)).thenReturn(false);

        assertFalse(listingService.canEditListing(sampleListing, authContext));
    }
}
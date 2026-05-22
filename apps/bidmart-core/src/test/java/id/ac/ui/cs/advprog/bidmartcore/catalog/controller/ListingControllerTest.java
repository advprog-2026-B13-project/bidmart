package id.ac.ui.cs.advprog.bidmartcore.catalog.controller;

import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingCreateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.ListingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ListingControllerTest {

    // ── Exception handler lokal disesuaikan untuk OPSI 2 (Simulasi Default Spring 500) ──
    @RestControllerAdvice
    static class TestExceptionHandler {
        @ExceptionHandler({
                SecurityException.class,
                IllegalArgumentException.class,
                IllegalStateException.class,
                RuntimeException.class
        })
        public ResponseEntity<String> handleAllExceptions(Exception ex) {
            // Simulasi bawaan Spring Boot yang melempar 500 untuk exception tanpa handler spesifik
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }

        // MethodArgumentTypeMismatchException biasanya otomatis di-handle Spring menjadi 400
        // karena ini adalah kesalahan format URL, bukan business logic
        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    private MockMvc mockMvc;

    @Mock
    private ListingService listingService;

    @Mock
    private AuthContext authContext;

    @InjectMocks
    private ListingController listingController;

    private UUID listingId;
    private UUID userId;
    private Listing sampleListing;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(listingController)
                .setControllerAdvice(new TestExceptionHandler())
                // Interceptor untuk mensimulasikan @RequirePermission
                // Jika request memuat header "USER", otomatis lempar SecurityException
                .addInterceptors(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                        if ("USER".equals(request.getHeader("X-User-Role"))) {
                            throw new SecurityException("Akses Ditolak");
                        }
                        return true;
                    }
                })
                .build();

        listingId = UUID.randomUUID();
        userId = UUID.randomUUID();

        sampleListing = new Listing();
        sampleListing.setId(listingId);
        sampleListing.setSellerId(userId);
        sampleListing.setTitle("Laptop ROG Strix G16");
        sampleListing.setCurrentPrice(new BigDecimal("22000000"));
        sampleListing.setStatus(ListingStatus.ACTIVE);
    }

    // ─────────────────────────── searchListings ───────────────────────────

    @Test
    @DisplayName("Negative Case [searchListings]: Mengembalikan early return Page.empty jika status bernilai DRAFT")
    void testSearchListingsDraftEarlyReturn() throws Exception {
        mockMvc.perform(get("/api/catalog/listings/search")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(listingService, never()).searchListings(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Edge Case [searchListings]: Menangani ralat Exception dari layer service secara aman (Expect 500)")
    void testSearchListingsServerError() throws Exception {
        when(listingService.searchListings(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/catalog/listings/search"))
                .andExpect(status().isInternalServerError());
    }

    // ─────────────────────────── getListingById ───────────────────────────

    @Test
    @DisplayName("Positive Case [getListingById]: Sukses mengambil detail produk aktif berdasarkan ID")
    void testGetListingByIdSuccess() throws Exception {
        when(listingService.getListingById(listingId)).thenReturn(sampleListing);

        mockMvc.perform(get("/api/catalog/listings/detail/{id}", listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Laptop ROG Strix G16"));
    }

    @Test
    @DisplayName("Negative Case [getListingById]: Mengembalikan status 404 jika produk berstatus DRAFT")
    void testGetListingByIdDraftReturnsNotFound() throws Exception {
        sampleListing.setStatus(ListingStatus.DRAFT);
        when(listingService.getListingById(listingId)).thenReturn(sampleListing);

        mockMvc.perform(get("/api/catalog/listings/detail/{id}", listingId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Edge Case [getListingById]: Melempar ralat client error jika service tidak menemukan ID produk (Expect 500)")
    void testGetListingByIdServiceException() throws Exception {
        when(listingService.getListingById(listingId))
                .thenThrow(new IllegalArgumentException("Not found"));

        mockMvc.perform(get("/api/catalog/listings/detail/{id}", listingId))
                .andExpect(status().isInternalServerError());
    }

    // ─────────────────────────── getListingByOwner ───────────────────────────

    @Test
    @DisplayName("Positive Case [getListingByOwner]: Owner asli sukses memanggil detail privat miliknya")
    void testGetListingByOwnerSuccess() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.getListingForOwner(listingId, userId)).thenReturn(sampleListing);

        mockMvc.perform(get("/api/catalog/listings/detail/{id}/owner", listingId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Negative Case [getListingByOwner]: Menolak akses jika requester bukan pemilik sah produk (Expect 500)")
    void testGetListingByOwnerAccessDenied() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.getListingForOwner(listingId, userId))
                .thenThrow(new SecurityException("Akses ditolak"));

        mockMvc.perform(get("/api/catalog/listings/detail/{id}/owner", listingId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Edge Case [getListingByOwner]: Menggagalkan request jika context userId bernilai null (Expect 500)")
    void testGetListingByOwnerNullUser() throws Exception {
        when(authContext.getUserId()).thenReturn(null);
        when(listingService.getListingForOwner(listingId, null))
                .thenThrow(new IllegalArgumentException("User tidak terautentikasi"));

        mockMvc.perform(get("/api/catalog/listings/detail/{id}/owner", listingId))
                .andExpect(status().isInternalServerError());
    }

    // ─────────────────────────── getMyListings ───────────────────────────

    @Test
    @DisplayName("Positive Case [getMyListings]: Sukses mengumpulkan semua produk dagangan milik saya")
    void testGetMyListingsSuccess() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.getListingsBySeller(userId)).thenReturn(List.of(sampleListing));

        mockMvc.perform(get("/api/catalog/listings/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Laptop ROG Strix G16"));
    }

    @Test
    @DisplayName("Negative Case [getMyListings]: Mengembalikan senarai kosong jika penjual belum mempublikasikan produk")
    void testGetMyListingsEmpty() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.getListingsBySeller(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/catalog/listings/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Edge Case [getMyListings]: Mengembalikan ralat internal server jika database timeout (Expect 500)")
    void testGetMyListingsServerError() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.getListingsBySeller(userId))
                .thenThrow(new RuntimeException("Timeout"));

        mockMvc.perform(get("/api/catalog/listings/mine"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Edge Case [getMyListings]: Menolak akses jika userId null saat memanggil daftar listing milik sendiri (Expect 500)")
    void testGetMyListingsNullUser() throws Exception {
        when(authContext.getUserId()).thenReturn(null);
        when(listingService.getListingsBySeller(null))
                .thenThrow(new IllegalArgumentException("User tidak terautentikasi"));

        mockMvc.perform(get("/api/catalog/listings/mine"))
                .andExpect(status().isInternalServerError());
    }

    // ─────────────────────────── createListing ───────────────────────────

    @Test
    @DisplayName("Negative Case [createListing]: Gagal membuat jika payload string kosong")
    void testCreateListingBadRequest() throws Exception {
        String invalidPayload = "{\"title\":\"\",\"description\":\"\"}";

        mockMvc.perform(post("/api/catalog/listings/create")
                        .header("X-User-Role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Edge Case [createListing]: Menolak pembuatan jika diakses oleh role USER (Expect 500)")
    void testCreateListingSecurityException() throws Exception {
        String payload = "{\"title\":\"ROG\",\"description\":\"Mulus\",\"startingPrice\":10000,\"categoryId\":1,\"minBidIncrement\":500,\"endTime\":\"2099-12-31T00:00:00\"}";

        mockMvc.perform(post("/api/catalog/listings/create")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isInternalServerError());
    }

    // ─────────────────────────── updateListing ───────────────────────────

    @Test
    @DisplayName("Negative Case [updateListing]: Gagal update listing karena role USER (Expect 500)")
    void testUpdateListingAccessDenied_NotSeller() throws Exception {
        String jsonRequest = "{\"title\":\"Update Title\"}";

        mockMvc.perform(put("/api/catalog/listings/update/{id}", listingId)
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isInternalServerError());
    }

    // ─────────────────────────── activateListing ───────────────────────────

    @Test
    @DisplayName("Positive Case [activateListing]: Owner berhasil mengaktifkan produk lelang draf")
    void testActivateListingSuccess() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.activateListing(listingId, userId)).thenReturn(sampleListing);

        mockMvc.perform(put("/api/catalog/listings/{id}/activate", listingId)
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Negative Case [activateListing]: Gagal aktivasi jika status data awal bukan DRAFT (Expect 500)")
    void testActivateListingInvalidState() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.activateListing(listingId, userId))
                .thenThrow(new IllegalStateException("Bukan draf"));

        mockMvc.perform(put("/api/catalog/listings/{id}/activate", listingId)
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Edge Case [activateListing]: Menangani error jika ID produk target salah (Expect 500)")
    void testActivateListingNotFound() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.activateListing(listingId, userId))
                .thenThrow(new IllegalArgumentException("Not Found"));

        mockMvc.perform(put("/api/catalog/listings/{id}/activate", listingId)
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Edge Case [activateListing]: Menolak aktivasi jika bukan pemilik listing (Expect 500)")
    void testActivateListingNotOwner() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.activateListing(listingId, userId))
                .thenThrow(new SecurityException("Bukan pemilik"));

        mockMvc.perform(put("/api/catalog/listings/{id}/activate", listingId)
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isInternalServerError());
    }

    // ─────────────────────────── closeListing ───────────────────────────

    @Test
    @DisplayName("Positive Case [closeListing]: Pemilik sukses menutup paksa lapak sebelum jatuh tempo")
    void testCloseListingSuccess() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.closeListing(listingId, userId)).thenReturn(sampleListing);

        mockMvc.perform(put("/api/catalog/listings/{id}/close", listingId)
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Negative Case [closeListing]: Menolak penutupan jika durasi tayang lelang telah kadaluwarsa (Expect 500)")
    void testCloseListingAlreadyExpired() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.closeListing(listingId, userId))
                .thenThrow(new IllegalStateException("Expired"));

        mockMvc.perform(put("/api/catalog/listings/{id}/close", listingId)
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Edge Case [closeListing]: Menolak penutupan jika otentikasi user terlepas (Expect 500)")
    void testCloseListingUnauthenticated() throws Exception {
        when(authContext.getUserId()).thenReturn(null);
        when(listingService.closeListing(listingId, null))
                .thenThrow(new IllegalArgumentException("User tidak terautentikasi"));

        mockMvc.perform(put("/api/catalog/listings/{id}/close", listingId)
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Edge Case [closeListing]: Menolak penutupan jika bukan pemilik listing (Expect 500)")
    void testCloseListingNotOwner() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.closeListing(listingId, userId))
                .thenThrow(new SecurityException("Bukan pemilik"));

        mockMvc.perform(put("/api/catalog/listings/{id}/close", listingId)
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isInternalServerError());
    }

    // ─────────────────────────── deleteListing ───────────────────────────

    @Test
    @DisplayName("Positive Case [deleteListing]: Owner berhasil menghapus data listing draf miliknya")
    void testDeleteListingSuccess() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        doNothing().when(listingService).deleteListing(listingId, userId);

        mockMvc.perform(delete("/api/catalog/listings/delete/{id}", listingId)
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Negative Case [deleteListing]: Gagal delete listing karena role USER (Expect 500)")
    void testDeleteListingAccessDenied_NotSeller() throws Exception {
        mockMvc.perform(delete("/api/catalog/listings/delete/{id}", listingId)
                        .header("X-User-Role", "USER"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Negative Case [deleteListing]: Gagal membatalkan barang jualan jika bidCount > 0 (Expect 500)")
    void testDeleteListingHasBidsConflict() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        doThrow(new IllegalStateException("Sudah ada penawaran aktif"))
                .when(listingService).deleteListing(listingId, userId);

        mockMvc.perform(delete("/api/catalog/listings/delete/{id}", listingId)
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Edge Case [deleteListing]: Menolak penghapusan jika dilemparkan oleh hacker (Expect 500)")
    void testDeleteListingNotOwner() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        doThrow(new SecurityException("Bukan pemilik"))
                .when(listingService).deleteListing(listingId, userId);

        mockMvc.perform(delete("/api/catalog/listings/delete/{id}", listingId)
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Edge Case [deleteListing]: Menolak penghapusan jika ID listing tidak ditemukan (Expect 500)")
    void testDeleteListingNotFound() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        doThrow(new IllegalArgumentException("Listing tidak ditemukan"))
                .when(listingService).deleteListing(listingId, userId);

        mockMvc.perform(delete("/api/catalog/listings/delete/{id}", listingId)
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isInternalServerError());
    }

    // ─────────────────────────── validateListingForBid ───────────────────────────

    @Test
    @DisplayName("Positive Case [validateListingForBid]: Mengembalikan true jika valid untuk proses bidding")
    void testValidateListingForBidTrue() throws Exception {
        when(listingService.isListingValidForBid(listingId)).thenReturn(true);

        mockMvc.perform(get("/api/catalog/listings/{id}/validate", listingId))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("Negative Case [validateListingForBid]: Mengembalikan false jika tidak halal ditawar")
    void testValidateListingForBidFalse() throws Exception {
        when(listingService.isListingValidForBid(listingId)).thenReturn(false);

        mockMvc.perform(get("/api/catalog/listings/{id}/validate", listingId))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("Edge Case [validateListingForBid]: Menolak komputasi jika format parameter ID hancur (Expect 400)")
    void testValidateListingForBidInvalidId() throws Exception {
        // Exception berupa format UUID URL salah dikelola menjadi 400
        mockMvc.perform(get("/api/catalog/listings/id-format-salah/validate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Edge Case [validateListingForBid]: Menangani exception dari service saat validasi (Expect 500)")
    void testValidateListingForBidServiceException() throws Exception {
        when(listingService.isListingValidForBid(listingId))
                .thenThrow(new IllegalArgumentException("Listing tidak ditemukan"));

        mockMvc.perform(get("/api/catalog/listings/{id}/validate", listingId))
                .andExpect(status().isInternalServerError());
    }
}
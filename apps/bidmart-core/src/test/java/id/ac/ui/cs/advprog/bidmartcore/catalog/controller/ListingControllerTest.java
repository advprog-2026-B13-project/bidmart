package id.ac.ui.cs.advprog.bidmartcore.catalog.controller;

import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.ListingService;
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

    @RestControllerAdvice
    static class TestExceptionHandler {

        @ExceptionHandler(SecurityException.class)
        public ResponseEntity<String> handleSecurity(SecurityException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<String> handleIllegalState(IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<String> handleRuntime(RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
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
    @DisplayName("Positive Case [searchListings]: Sukses mencari listing dengan parameter filter lengkap")
    void testSearchListingsSuccess() throws Exception {
        Page<Listing> page = new PageImpl<>(List.of(sampleListing));
        when(listingService.searchListings(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/catalog/listings/search")
                        .param("keyword", "ROG")
                        .param("minPrice", "10000000"))
                .andExpect(status().isOk());
    }

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
    @DisplayName("Edge Case [searchListings]: Menangani ralat Exception dari layer service secara aman")
    void testSearchListingsServerError() throws Exception {
        when(listingService.searchListings(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/catalog/listings/search"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Edge Case [searchListings]: Sukses mencari listing dengan semua filter termasuk maxPrice dan categoryId")
    void testSearchListingsAllFilters() throws Exception {
        Page<Listing> page = new PageImpl<>(List.of(sampleListing));
        when(listingService.searchListings(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/catalog/listings/search")
                        .param("keyword", "ROG")
                        .param("minPrice", "10000000")
                        .param("maxPrice", "30000000")
                        .param("categoryId", "2")
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());
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
    @DisplayName("Edge Case [getListingById]: Melempar ralat client error jika service tidak menemukan ID produk")
    void testGetListingByIdServiceException() throws Exception {
        when(listingService.getListingById(listingId))
                .thenThrow(new IllegalArgumentException("Not found"));

        mockMvc.perform(get("/api/catalog/listings/detail/{id}", listingId))
                .andExpect(status().isBadRequest());
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
    @DisplayName("Negative Case [getListingByOwner]: Menolak akses jika requester bukan pemilik sah produk")
    void testGetListingByOwnerAccessDenied() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.getListingForOwner(listingId, userId))
                .thenThrow(new SecurityException("Akses ditolak"));

        mockMvc.perform(get("/api/catalog/listings/detail/{id}/owner", listingId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Edge Case [getListingByOwner]: Menggagalkan request jika context userId bernilai null")
    void testGetListingByOwnerNullUser() throws Exception {
        when(authContext.getUserId()).thenReturn(null);
        when(listingService.getListingForOwner(listingId, null))
                .thenThrow(new IllegalArgumentException("User tidak terautentikasi"));

        mockMvc.perform(get("/api/catalog/listings/detail/{id}/owner", listingId))
                .andExpect(status().isBadRequest());
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
    @DisplayName("Edge Case [getMyListings]: Mengembalikan ralat internal server jika database timeout")
    void testGetMyListingsServerError() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.getListingsBySeller(userId))
                .thenThrow(new RuntimeException("Timeout"));

        mockMvc.perform(get("/api/catalog/listings/mine"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Edge Case [getMyListings]: Menolak akses jika userId null saat memanggil daftar listing milik sendiri")
    void testGetMyListingsNullUser() throws Exception {
        when(authContext.getUserId()).thenReturn(null);
        when(listingService.getListingsBySeller(null))
                .thenThrow(new IllegalArgumentException("User tidak terautentikasi"));

        mockMvc.perform(get("/api/catalog/listings/mine"))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────── createListing ───────────────────────────

    @Test
    @DisplayName("Positive Case [createListing]: Berhasil menerbitkan produk baru dalam database")
    void testCreateListingSuccess() throws Exception {
        String payload = "{\"title\":\"ROG\",\"description\":\"Mulus\",\"startingPrice\":10000,\"categoryId\":1}";
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.createListing(any(), eq(userId))).thenReturn(sampleListing);

        mockMvc.perform(post("/api/catalog/listings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Negative Case [createListing]: Gagal membuat jika payload string kosong")
    void testCreateListingBadRequest() throws Exception {
        String invalidPayload = "{\"title\":\"\",\"description\":\"\"}";

        mockMvc.perform(post("/api/catalog/listings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Edge Case [createListing]: Menolak eksekusi pembuatan jika otentikasi gagal")
    void testCreateListingUnauthenticated() throws Exception {
        String payload = "{\"title\":\"ROG\",\"description\":\"Mulus\",\"startingPrice\":10000,\"categoryId\":1}";
        when(authContext.getUserId()).thenReturn(null);
        when(listingService.createListing(any(), eq(null)))
                .thenThrow(new IllegalArgumentException("User tidak terautentikasi"));

        mockMvc.perform(post("/api/catalog/listings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Edge Case [createListing]: Menolak pembuatan jika service melempar SecurityException")
    void testCreateListingSecurityException() throws Exception {
        String payload = "{\"title\":\"ROG\",\"description\":\"Mulus\",\"startingPrice\":10000,\"categoryId\":1}";
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.createListing(any(), eq(userId)))
                .thenThrow(new SecurityException("Tidak memiliki izin"));

        mockMvc.perform(post("/api/catalog/listings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────── updateListing ───────────────────────────

    @Test
    @DisplayName("Positive Case [updateListing]: Berhasil memperbarui kelengkapan deskripsi listing")
    void testUpdateListingSuccess() throws Exception {
        String payload = "{\"description\":\"Update Deskripsi Baru\"}";
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.updateListing(eq(listingId), eq(userId), any())).thenReturn(sampleListing);

        mockMvc.perform(put("/api/catalog/listings/update/{id}", listingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Negative Case [updateListing]: Menolak pembaharuan jika pelempar request bukan pemilik asli")
    void testUpdateListingNotOwner() throws Exception {
        String payload = "{\"description\":\"Hacker Test\"}";
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.updateListing(eq(listingId), eq(userId), any()))
                .thenThrow(new SecurityException("Akses ditolak"));

        mockMvc.perform(put("/api/catalog/listings/update/{id}", listingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Edge Case [updateListing]: Menolak modifikasi jika state siklus lelang sudah berjalan final")
    void testUpdateListingFinalStateConflict() throws Exception {
        String payload = "{\"description\":\"Update State Akhir\"}";
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.updateListing(eq(listingId), eq(userId), any()))
                .thenThrow(new IllegalStateException("Sudah ada bid"));

        mockMvc.perform(put("/api/catalog/listings/update/{id}", listingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Edge Case [updateListing]: Menolak pembaruan jika ID listing tidak ditemukan")
    void testUpdateListingNotFound() throws Exception {
        String payload = "{\"description\":\"Update Tidak Ada\"}";
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.updateListing(eq(listingId), eq(userId), any()))
                .thenThrow(new IllegalArgumentException("Listing tidak ditemukan"));

        mockMvc.perform(put("/api/catalog/listings/update/{id}", listingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────── activateListing ───────────────────────────

    @Test
    @DisplayName("Positive Case [activateListing]: Owner berhasil mengaktifkan produk lelang draf")
    void testActivateListingSuccess() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.activateListing(listingId, userId)).thenReturn(sampleListing);

        mockMvc.perform(put("/api/catalog/listings/{id}/activate", listingId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Negative Case [activateListing]: Gagal aktivasi jika status data awal bukan DRAFT")
    void testActivateListingInvalidState() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.activateListing(listingId, userId))
                .thenThrow(new IllegalStateException("Bukan draf"));

        mockMvc.perform(put("/api/catalog/listings/{id}/activate", listingId))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Edge Case [activateListing]: Menangani error jika ID produk target salah")
    void testActivateListingNotFound() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.activateListing(listingId, userId))
                .thenThrow(new IllegalArgumentException("Not Found"));

        mockMvc.perform(put("/api/catalog/listings/{id}/activate", listingId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Edge Case [activateListing]: Menolak aktivasi jika bukan pemilik listing")
    void testActivateListingNotOwner() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.activateListing(listingId, userId))
                .thenThrow(new SecurityException("Bukan pemilik"));

        mockMvc.perform(put("/api/catalog/listings/{id}/activate", listingId))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────── closeListing ───────────────────────────

    @Test
    @DisplayName("Positive Case [closeListing]: Pemilik sukses menutup paksa lapak sebelum jatuh tempo")
    void testCloseListingSuccess() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.closeListing(listingId, userId)).thenReturn(sampleListing);

        mockMvc.perform(put("/api/catalog/listings/{id}/close", listingId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Negative Case [closeListing]: Menolak penutupan jika durasi tayang lelang telah kadaluwarsa")
    void testCloseListingAlreadyExpired() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.closeListing(listingId, userId))
                .thenThrow(new IllegalStateException("Expired"));

        mockMvc.perform(put("/api/catalog/listings/{id}/close", listingId))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Edge Case [closeListing]: Menolak penutupan jika otentikasi user terlepas")
    void testCloseListingUnauthenticated() throws Exception {
        when(authContext.getUserId()).thenReturn(null);
        when(listingService.closeListing(listingId, null))
                .thenThrow(new IllegalArgumentException("User tidak terautentikasi"));

        mockMvc.perform(put("/api/catalog/listings/{id}/close", listingId))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Edge Case [closeListing]: Menolak penutupan jika bukan pemilik listing")
    void testCloseListingNotOwner() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.closeListing(listingId, userId))
                .thenThrow(new SecurityException("Bukan pemilik"));

        mockMvc.perform(put("/api/catalog/listings/{id}/close", listingId))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────── deleteListing ───────────────────────────

    @Test
    @DisplayName("Positive Case [deleteListing]: Owner berhasil menghapus data listing draf miliknya")
    void testDeleteListingSuccess() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        doNothing().when(listingService).deleteListing(listingId, userId);

        mockMvc.perform(delete("/api/catalog/listings/delete/{id}", listingId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Negative Case [deleteListing]: Gagal membatalkan barang jualan jika bidCount > 0")
    void testDeleteListingHasBidsConflict() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        doThrow(new IllegalStateException("Sudah ada penawaran aktif"))
                .when(listingService).deleteListing(listingId, userId);

        mockMvc.perform(delete("/api/catalog/listings/delete/{id}", listingId))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Edge Case [deleteListing]: Menolak penghapusan jika dilemparkan oleh hacker")
    void testDeleteListingNotOwner() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        doThrow(new SecurityException("Bukan pemilik"))
                .when(listingService).deleteListing(listingId, userId);

        mockMvc.perform(delete("/api/catalog/listings/delete/{id}", listingId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Edge Case [deleteListing]: Menolak penghapusan jika ID listing tidak ditemukan")
    void testDeleteListingNotFound() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        doThrow(new IllegalArgumentException("Listing tidak ditemukan"))
                .when(listingService).deleteListing(listingId, userId);

        mockMvc.perform(delete("/api/catalog/listings/delete/{id}", listingId))
                .andExpect(status().isBadRequest());
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
    @DisplayName("Edge Case [validateListingForBid]: Menolak komputasi jika format parameter ID hancur")
    void testValidateListingForBidInvalidId() throws Exception {
        mockMvc.perform(get("/api/catalog/listings/id-format-salah/validate"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Edge Case [validateListingForBid]: Menangani exception dari service saat validasi")
    void testValidateListingForBidServiceException() throws Exception {
        when(listingService.isListingValidForBid(listingId))
                .thenThrow(new IllegalArgumentException("Listing tidak ditemukan"));

        mockMvc.perform(get("/api/catalog/listings/{id}/validate", listingId))
                .andExpect(status().isBadRequest());
    }
}
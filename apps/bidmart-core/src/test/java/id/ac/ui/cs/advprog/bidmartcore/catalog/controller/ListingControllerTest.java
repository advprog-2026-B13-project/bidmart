package id.ac.ui.cs.advprog.bidmartcore.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.ListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ListingController.class)
class ListingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListingService listingService;

    @MockBean
    private AuthContext authContext;

    @Autowired
    private ObjectMapper objectMapper;

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
        sampleListing.setTitle("Laptop ROG Strix G16");
        sampleListing.setCurrentPrice(new BigDecimal("22000000"));
        sampleListing.setStatus(ListingStatus.ACTIVE);
    }

    @Test
    @DisplayName("Positive Case [searchListings]: Sukses mencari listing dengan parameter filter lengkap")
    void testSearchListingsSuccess() throws Exception {
        Page<Listing> page = new PageImpl<>(List.of(sampleListing));
        when(listingService.searchListings(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

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
                .andExpect(status().is5xxServerError());
    }

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
        when(listingService.getListingById(listingId)).thenThrow(new IllegalArgumentException("Not found"));

        mockMvc.perform(get("/api/catalog/listings/detail/{id}", listingId))
                .andExpect(status().is5xxClientError());
    }

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
        when(listingService.getListingForOwner(listingId, userId)).thenThrow(new SecurityException("Akses ditolak"));

        mockMvc.perform(get("/api/catalog/listings/detail/{id}/owner", listingId))
                .andExpect(status().is5xxClientError());
    }

    @Test
    @DisplayName("Edge Case [getListingByOwner]: Menggagalkan request jika context userId bernilai null")
    void testGetListingByOwnerNullUser() throws Exception {
        when(authContext.getUserId()).thenReturn(null);

        mockMvc.perform(get("/api/catalog/listings/detail/{id}/owner", listingId))
                .andExpect(status().is5xxClientError());
    }

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
        when(listingService.getListingsBySeller(userId)).thenThrow(new RuntimeException("Timeout"));

        mockMvc.perform(get("/api/catalog/listings/mine"))
                .andExpect(status().is5xxServerError());
    }

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
        String payload = "{\"title\":\"ROG\",\"description\":\"Mulus\"}";
        when(authContext.getUserId()).thenReturn(null);

        mockMvc.perform(post("/api/catalog/listings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().is5xxClientError());
    }

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
                .andExpect(status().is5xxClientError());
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
                .andExpect(status().is5xxClientError());
    }

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
        when(listingService.activateListing(listingId, userId)).thenThrow(new IllegalStateException("Bukan draf"));

        mockMvc.perform(put("/api/catalog/listings/{id}/activate", listingId))
                .andExpect(status().is5xxClientError());
    }

    @Test
    @DisplayName("Edge Case [activateListing]: Menangani error jika ID produk target salah")
    void testActivateListingNotFound() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.activateListing(listingId, userId)).thenThrow(new IllegalArgumentException("Not Found"));

        mockMvc.perform(put("/api/catalog/listings/{id}/activate", listingId))
                .andExpect(status().is5xxClientError());
    }

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
        when(listingService.closeListing(listingId, userId)).thenThrow(new IllegalStateException("Expired"));

        mockMvc.perform(put("/api/catalog/listings/{id}/close", listingId))
                .andExpect(status().is5xxClientError());
    }

    @Test
    @DisplayName("Edge Case [closeListing]: Menolak penutupan jika otentikasi user terlepas")
    void testCloseListingUnauthenticated() throws Exception {
        when(authContext.getUserId()).thenReturn(null);

        mockMvc.perform(put("/api/catalog/listings/{id}/close", listingId))
                .andExpect(status().is5xxClientError());
    }

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
        doThrow(new IllegalStateException("Sudah ada penawaran aktif")).when(listingService).deleteListing(listingId, userId);

        mockMvc.perform(delete("/api/catalog/listings/delete/{id}", listingId))
                .andExpect(status().is5xxClientError());
    }

    @Test
    @DisplayName("Edge Case [deleteListing]: Menolak penghapusan jika dilemparkan oleh hacker")
    void testDeleteListingNotOwner() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        doThrow(new SecurityException("Bukan pemilik")).when(listingService).deleteListing(listingId, userId);

        mockMvc.perform(delete("/api/catalog/listings/delete/{id}", listingId))
                .andExpect(status().is5xxClientError());
    }

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
                .andExpect(status().is5xxClientError());
    }

    @Test
    @DisplayName("Positive Case [takeDownListing]: Admin sukses melakukan takedown darurat dengan mencantumkan alasan")
    void testTakeDownListingSuccess() throws Exception {
        String jsonRequest = "{\"takedownReason\":\"Pelanggaran hak cipta\"}";
        when(authContext.getUserId()).thenReturn(userId);
        sampleListing.setStatus(ListingStatus.CLOSED);
        when(listingService.takeDownListing(eq(listingId), any(String.class), eq(userId))).thenReturn(sampleListing);

        mockMvc.perform(patch("/api/catalog/listings/{id}/takedown", listingId)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @DisplayName("Negative Case [takeDownListing]: Menggagalkan takedown jika role panggil adalah USER biasa")
    void testTakeDownListingAccessDenied() throws Exception {
        String jsonRequest = "{\"takedownReason\":\"Iseng Takedown\"}";

        mockMvc.perform(patch("/api/catalog/listings/{id}/takedown", listingId)
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().is5xxClientError());
    }

    @Test
    @DisplayName("Edge Case [takeDownListing]: Menolak moderasi jika produk lelang telah memasuki status final")
    void testTakeDownListingFinalStateConflict() throws Exception {
        String jsonRequest = "{\"takedownReason\":\"Terlambat\"}";
        when(authContext.getUserId()).thenReturn(userId);
        when(listingService.takeDownListing(eq(listingId), any(String.class), eq(userId)))
                .thenThrow(new IllegalStateException("Status final"));

        mockMvc.perform(patch("/api/catalog/listings/{id}/takedown", listingId)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().is5xxClientError());
    }
}
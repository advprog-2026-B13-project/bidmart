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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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
    @DisplayName("Positive Case [validateListingForBid]: Mengembalikan true jika listing aktif dan berdurasi valid")
    void testValidateListingForBidTrue() throws Exception {
        when(listingService.isListingValidForBid(listingId)).thenReturn(true);
        mockMvc.perform(get("/api/catalog/listings/{id}/validate", listingId))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(listingService, times(1)).isListingValidForBid(listingId);
    }

    @Test
    @DisplayName("Negative Case [validateListingForBid]: Mengembalikan false jika listing sudah expired atau tidak aktif")
    void testValidateListingForBidFalse() throws Exception {
        when(listingService.isListingValidForBid(listingId)).thenReturn(false);
        mockMvc.perform(get("/api/catalog/listings/{id}/validate", listingId))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(listingService, times(1)).isListingValidForBid(listingId);
    }

    @Test
    @DisplayName("Edge Case [validateListingForBid]: Menangani error pelemparan ralat exception jika terjadi kegagalan database")
    void testValidateListingForBidException() throws Exception {
        when(listingService.isListingValidForBid(listingId)).thenThrow(new IllegalArgumentException("Listing tidak ditemukan"));
        mockMvc.perform(get("/api/catalog/listings/{id}/validate", listingId))
                .andExpect(status().is4xxClientError());

        verify(listingService, times(1)).isListingValidForBid(listingId);
    }

    @Test
    @DisplayName("Positive Case [takeDownListing]: Admin sukses melakukan takedown paksa dengan mencantumkan alasan")
    void testTakeDownListingSuccess() throws Exception {
        String jsonRequest = "{\"takedownReason\":\"Melanggar TOS Kategori Barang Terlarang\"}";
        when(authContext.getUserId()).thenReturn(userId);

        sampleListing.setStatus(ListingStatus.CLOSED);
        when(listingService.takeDownListing(eq(listingId), any(String.class), eq(userId))).thenReturn(sampleListing);
        mockMvc.perform(patch("/api/catalog/listings/{id}/takedown", listingId)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        verify(listingService, times(1)).takeDownListing(eq(listingId), any(String.class), eq(userId));
    }

    @Test
    @DisplayName("Negative Case [takeDownListing]: Menolak aksi takedown jika role panggil bukan ADMIN")
    void testTakeDownListingAccessDenied() throws Exception {
        String jsonRequest = "{\"takedownReason\":\"Alasan Iseng\"}";
        mockMvc.perform(patch("/api/catalog/listings/{id}/takedown", listingId)
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().is4xxClientError());

        verify(listingService, never()).takeDownListing(any(), any(), any());
    }

    @Test
    @DisplayName("Edge Case [takeDownListing]: Gagal takedown jika produk lelang sudah terlanjur dalam status final (WON/UNSOLD)")
    void testTakeDownListingFinalStateConflict() throws Exception {
        String jsonRequest = "{\"takedownReason\":\"Takedown Terlambat\"}";
        when(authContext.getUserId()).thenReturn(userId);

        when(listingService.takeDownListing(eq(listingId), any(String.class), eq(userId)))
                .thenThrow(new IllegalStateException("Validasi Gagal: Listing yang sudah dalam status final tidak dapat di-takedown."));
        mockMvc.perform(patch("/api/catalog/listings/{id}/takedown", listingId)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().is4xxClientError());

        verify(listingService, times(1)).takeDownListing(eq(listingId), any(String.class), eq(userId));
    }

    @Test
    @DisplayName("Positive Case [deleteListing]: Penjual sukses menghapus listing miliknya yang belum ada penawaran")
    void testDeleteListingSuccess() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        doNothing().when(listingService).deleteListing(listingId, userId);
        mockMvc.perform(delete("/api/catalog/listings/delete/{id}", listingId))
                .andExpect(status().isNoContent());

        verify(listingService, times(1)).deleteListing(listingId, userId);
    }

    @Test
    @DisplayName("Negative Case [deleteListing]: Gagal menghapus jika listing sudah mengikat penawaran aktif (bidCount > 0)")
    void testDeleteListingHasBidsConflict() throws Exception {
        when(authContext.getUserId()).thenReturn(userId);
        doThrow(new IllegalStateException("Listing tidak bisa dihapus karena sudah ada bid."))
                .when(listingService).deleteListing(listingId, userId);

        mockMvc.perform(delete("/api/catalog/listings/delete/{id}", listingId))
                .andExpect(status().is4xxClientError());

        verify(listingService, times(1)).deleteListing(listingId, userId);
    }

    @Test
    @DisplayName("Edge Case [deleteListing]: Gagal menghapus jika pengguna yang mencoba menghapus bukan owner asli produk")
    void testDeleteListingNotOwnerConflict() throws Exception {
        UUID maliciousUserId = UUID.randomUUID();
        when(authContext.getUserId()).thenReturn(maliciousUserId);
        doThrow(new IllegalArgumentException("Anda bukan pemilik listing ini"))
                .when(listingService).deleteListing(listingId, maliciousUserId);
        mockMvc.perform(delete("/api/catalog/listings/delete/{id}", listingId))
                .andExpect(status().is4xxClientError());

        verify(listingService, times(1)).deleteListing(listingId, maliciousUserId);
    }
}
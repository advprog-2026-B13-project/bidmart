package id.ac.ui.cs.advprog.bidmartcore.catalog.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ListingTest {

    private Listing listing;
    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category();
        sampleCategory.setId(1);
        sampleCategory.setName("Elektronik");

        listing = new Listing();
        listing.setId(UUID.randomUUID());
        listing.setSellerId(UUID.randomUUID());
        listing.setCategory(sampleCategory);
        listing.setTitle("MacBook Pro M3");
        listing.setDescription("Mulus 99% lengkap dengan box.");
        listing.setStartingPrice(new BigDecimal("25000000"));
        listing.setStatus(ListingStatus.DRAFT);
    }

    @Test
    @DisplayName("Positive Case: Sukses inisialisasi default values dan validasi waktu lelang via prePersistSetup")
    void testPrePersistSetupSuccess() {
        LocalDateTime now = LocalDateTime.now();
        listing.setStartTime(now);
        listing.setEndTime(now.plusDays(3));
        listing.prePersistSetup();
        assertEquals(listing.getStartingPrice(), listing.getCurrentPrice(), "currentPrice harus disamakan dengan startingPrice secara default");
        assertEquals(BigDecimal.ONE, listing.getMinBidIncrement(), "minBidIncrement secara default harus bernilai BigDecimal.ONE");
        assertEquals(0, listing.getBidCount(), "bidCount lelang baru secara default harus bernilai 0");
        assertNotNull(listing.getCreatedAt(), "Field createdAt harus di-generate otomatis");
        assertNotNull(listing.getUpdatedAt(), "Field updatedAt harus di-generate otomatis");
    }

    @Test
    @DisplayName("Negative Case: Gagal validasi jika waktu selesai lelang mendahului waktu mulai")
    void testPrePersistSetupInvalidTimesThrowsException() {
        LocalDateTime now = LocalDateTime.now();
        listing.setStartTime(now);
        listing.setEndTime(now.minusHours(2));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            listing.prePersistSetup();
        }, "Harus menolak persist data jika urutan waktu lelang terbalik");

        assertEquals("Waktu selesai lelang harus setelah waktu mulai", exception.getMessage());
    }
}
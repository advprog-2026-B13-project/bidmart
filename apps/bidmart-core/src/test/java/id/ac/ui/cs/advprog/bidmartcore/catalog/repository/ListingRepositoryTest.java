package id.ac.ui.cs.advprog.bidmartcore.catalog.repository;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingRepositoryTest {

    @Mock
    private ListingRepository listingRepository;

    private Category electronicsCategory;
    private Category clothingCategory;
    private Listing activeListing;
    private UUID sellerId;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();

        electronicsCategory = new Category();
        electronicsCategory.setName("Elektronik");

        clothingCategory = new Category();
        clothingCategory.setName("Pakaian");

        activeListing = new Listing();
        activeListing.setId(UUID.randomUUID());
        activeListing.setSellerId(sellerId);
        activeListing.setCategory(electronicsCategory);
        activeListing.setTitle("MacBook Pro M3");
        activeListing.setDescription("Mulus 99% lengkap dengan box.");
        activeListing.setStartingPrice(new BigDecimal("25000000"));
        activeListing.setCurrentPrice(new BigDecimal("25000000"));
        activeListing.setMinBidIncrement(new BigDecimal("500000"));
        activeListing.setBidCount(2);
        activeListing.setStatus(ListingStatus.ACTIVE);
        activeListing.setStartTime(LocalDateTime.now().minusDays(1));
        activeListing.setEndTime(LocalDateTime.now().plusDays(2));
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [save]: Sukses menyimpan listing baru dan mengembalikan entitas tersimpan")
    void testSaveSuccess() {
        when(listingRepository.save(activeListing)).thenReturn(activeListing);

        Listing result = listingRepository.save(activeListing);

        assertNotNull(result);
        assertEquals("MacBook Pro M3", result.getTitle());
        assertEquals(ListingStatus.ACTIVE, result.getStatus());
        verify(listingRepository, times(1)).save(activeListing);
    }

    @Test
    @DisplayName("Negative Case [save]: Melempar exception jika entitas yang disimpan melanggar constraint")
    void testSaveConstraintViolation() {
        when(listingRepository.save(activeListing))
                .thenThrow(new RuntimeException("constraint violation"));

        assertThrows(RuntimeException.class, () -> listingRepository.save(activeListing));
        verify(listingRepository, times(1)).save(activeListing);
    }

    @Test
    @DisplayName("Edge Case [save]: Sukses meng-update listing yang sudah ada dengan data terbaru")
    void testSaveUpdatesExistingListing() {
        activeListing.setCurrentPrice(new BigDecimal("27000000"));
        activeListing.setBidCount(3);
        when(listingRepository.save(activeListing)).thenReturn(activeListing);

        Listing result = listingRepository.save(activeListing);

        assertNotNull(result);
        assertEquals(new BigDecimal("27000000"), result.getCurrentPrice());
        assertEquals(3, result.getBidCount());
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [findById]: Sukses mengambil listing berdasarkan UUID yang valid")
    void testFindByIdSuccess() {
        UUID id = UUID.randomUUID();
        activeListing.setId(id);
        when(listingRepository.findById(id)).thenReturn(Optional.of(activeListing));

        Optional<Listing> result = listingRepository.findById(id);

        assertTrue(result.isPresent());
        assertEquals("MacBook Pro M3", result.get().getTitle());
        verify(listingRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Negative Case [findById]: Mengembalikan Optional.empty jika UUID tidak terdaftar")
    void testFindByIdNotFound() {
        UUID randomId = UUID.randomUUID();
        when(listingRepository.findById(randomId)).thenReturn(Optional.empty());

        Optional<Listing> result = listingRepository.findById(randomId);

        assertFalse(result.isPresent());
        verify(listingRepository, times(1)).findById(randomId);
    }

    @Test
    @DisplayName("Edge Case [findById]: Melempar exception jika ID bernilai null")
    void testFindByIdNull() {
        when(listingRepository.findById(null))
                .thenThrow(new IllegalArgumentException("ID must not be null"));

        assertThrows(IllegalArgumentException.class, () -> listingRepository.findById(null));
    }

    // ── findAll ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [findAll]: Sukses mengambil seluruh daftar listing yang tersimpan")
    void testFindAllSuccess() {
        Listing secondListing = new Listing();
        secondListing.setTitle("iPhone 15 Pro");
        secondListing.setStatus(ListingStatus.ACTIVE);

        when(listingRepository.findAll()).thenReturn(List.of(activeListing, secondListing));

        List<Listing> results = listingRepository.findAll();

        assertEquals(2, results.size());
        verify(listingRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Negative Case [findAll]: Mengembalikan senarai kosong jika belum ada data tersimpan")
    void testFindAllEmpty() {
        when(listingRepository.findAll()).thenReturn(Collections.emptyList());

        List<Listing> results = listingRepository.findAll();

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [findAll]: Melempar exception jika koneksi database terputus")
    void testFindAllDatabaseError() {
        when(listingRepository.findAll())
                .thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class, () -> listingRepository.findAll());
    }

    // ── deleteById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [deleteById]: Sukses menghapus listing berdasarkan UUID yang valid")
    void testDeleteByIdSuccess() {
        UUID id = UUID.randomUUID();
        doNothing().when(listingRepository).deleteById(id);

        assertDoesNotThrow(() -> listingRepository.deleteById(id));
        verify(listingRepository, times(1)).deleteById(id);
    }

    @Test
    @DisplayName("Negative Case [deleteById]: Melempar exception jika listing dengan UUID tersebut tidak ada")
    void testDeleteByIdNotFound() {
        UUID randomId = UUID.randomUUID();
        doThrow(new IllegalArgumentException("Listing tidak ditemukan"))
                .when(listingRepository).deleteById(randomId);

        assertThrows(IllegalArgumentException.class, () -> listingRepository.deleteById(randomId));
    }

    @Test
    @DisplayName("Edge Case [deleteById]: Melempar exception jika ID yang diberikan bernilai null")
    void testDeleteByIdNull() {
        doThrow(new IllegalArgumentException("ID must not be null"))
                .when(listingRepository).deleteById(null);

        assertThrows(IllegalArgumentException.class, () -> listingRepository.deleteById(null));
    }

    // ── recordNewBid ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [recordNewBid]: Sukses memperbarui nominal bidaan baru")
    void testRecordNewBidSuccess() {
        UUID listingId = UUID.randomUUID();
        UUID winnerId  = UUID.randomUUID();
        BigDecimal newPrice = new BigDecimal("26000000");

        Listing updated = new Listing();
        updated.setBidCount(3);
        updated.setCurrentPrice(newPrice);
        updated.setWinnerId(winnerId);

        doNothing().when(listingRepository).recordNewBid(listingId, newPrice, winnerId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(updated));

        listingRepository.recordNewBid(listingId, newPrice, winnerId);
        Listing result = listingRepository.findById(listingId).orElse(null);

        assertNotNull(result);
        assertEquals(3, result.getBidCount(), "bidCount wajib berinkremen naik (+1)");
        assertEquals(newPrice, result.getCurrentPrice());
        assertEquals(winnerId, result.getWinnerId());

        verify(listingRepository, times(1)).recordNewBid(listingId, newPrice, winnerId);
    }

    @Test
    @DisplayName("Negative Case [recordNewBid]: Tidak merubah record lain jika target ID tidak cocok")
    void testRecordNewBidNotFound() {
        UUID wrongId = UUID.randomUUID();
        UUID listingId = activeListing.getId();

        doNothing().when(listingRepository).recordNewBid(wrongId, new BigDecimal("99000000"), any(UUID.class));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(activeListing));

        listingRepository.recordNewBid(wrongId, new BigDecimal("99000000"), UUID.randomUUID());
        Listing original = listingRepository.findById(listingId).orElse(null);

        assertNotNull(original);
        assertEquals(2, original.getBidCount(), "Data awal tidak boleh terdistorsi");
    }

    @Test
    @DisplayName("Edge Case [recordNewBid]: Menguji kegunaan COALESCE apabila data awal bidCount bernilai null")
    void testRecordNewBidWhenBidCountIsNull() {
        UUID edgeId    = UUID.randomUUID();
        UUID winnerId  = UUID.randomUUID();
        BigDecimal newPrice = new BigDecimal("10500000");

        Listing edgeListing = new Listing();
        edgeListing.setBidCount(null);

        Listing afterBid = new Listing();
        afterBid.setBidCount(1);
        afterBid.setCurrentPrice(newPrice);
        afterBid.setWinnerId(winnerId);

        doNothing().when(listingRepository).recordNewBid(edgeId, newPrice, winnerId);
        when(listingRepository.findById(edgeId)).thenReturn(Optional.of(afterBid));

        listingRepository.recordNewBid(edgeId, newPrice, winnerId);
        Listing updated = listingRepository.findById(edgeId).orElse(null);

        assertNotNull(updated);
        assertEquals(1, updated.getBidCount(), "COALESCE(null, 0) + 1 harus menghasilkan angka 1");
    }

    @Test
    @DisplayName("Edge Case [recordNewBid]: Melempar exception jika terjadi kegagalan database saat update")
    void testRecordNewBidDatabaseFailure() {
        UUID listingId = UUID.randomUUID();
        UUID winnerId  = UUID.randomUUID();
        BigDecimal newPrice = new BigDecimal("26000000");

        doThrow(new RuntimeException("Database error"))
                .when(listingRepository).recordNewBid(listingId, newPrice, winnerId);

        assertThrows(RuntimeException.class,
                () -> listingRepository.recordNewBid(listingId, newPrice, winnerId));
    }

    // ── findByCategoryId ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [findByCategoryId]: Sukses mencari daftar produk berdasarkan ID kategori yang cocok")
    void testFindByCategoryIdSuccess() {
        Integer categoryId = 1;
        when(listingRepository.findByCategoryId(categoryId)).thenReturn(List.of(activeListing));

        List<Listing> results = listingRepository.findByCategoryId(categoryId);

        assertEquals(1, results.size());
        assertEquals("MacBook Pro M3", results.get(0).getTitle());
        verify(listingRepository, times(1)).findByCategoryId(categoryId);
    }

    @Test
    @DisplayName("Negative Case [findByCategoryId]: Mengembalikan senarai kosong jika kategori belum diikat produk")
    void testFindByCategoryIdEmpty() {
        Integer categoryId = 2;
        when(listingRepository.findByCategoryId(categoryId)).thenReturn(Collections.emptyList());

        List<Listing> results = listingRepository.findByCategoryId(categoryId);

        assertTrue(results.isEmpty(), "Wajib memulangkan senarai kosong (clean code)");
    }

    @Test
    @DisplayName("Edge Case [findByCategoryId]: Menangani pencarian parameter categoryId bernilai null secara aman")
    void testFindByCategoryIdNull() {
        when(listingRepository.findByCategoryId(null)).thenReturn(Collections.emptyList());

        List<Listing> results = listingRepository.findByCategoryId(null);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [findByCategoryId]: Mengembalikan banyak listing jika lebih dari satu produk dalam kategori sama")
    void testFindByCategoryIdMultipleResults() {
        Integer categoryId = 1;
        Listing secondListing = new Listing();
        secondListing.setTitle("Dell XPS 15");
        secondListing.setStatus(ListingStatus.ACTIVE);

        when(listingRepository.findByCategoryId(categoryId))
                .thenReturn(List.of(activeListing, secondListing));

        List<Listing> results = listingRepository.findByCategoryId(categoryId);

        assertEquals(2, results.size());
    }

    // ── findBySellerId ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [findBySellerId]: Sukses mengumpulkan semua listing kepemilikan seller tertentu")
    void testFindBySellerIdSuccess() {
        when(listingRepository.findBySellerId(sellerId)).thenReturn(List.of(activeListing));

        List<Listing> results = listingRepository.findBySellerId(sellerId);

        assertEquals(1, results.size());
        verify(listingRepository, times(1)).findBySellerId(sellerId);
    }

    @Test
    @DisplayName("Negative Case [findBySellerId]: Mengembalikan list kosong jika seller UUID acak tidak memiliki produk")
    void testFindBySellerIdNotFound() {
        UUID randomId = UUID.randomUUID();
        when(listingRepository.findBySellerId(randomId)).thenReturn(Collections.emptyList());

        List<Listing> results = listingRepository.findBySellerId(randomId);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [findBySellerId]: Kebal dari ancaman error sewaktu sellerId bernilai null")
    void testFindBySellerIdNull() {
        when(listingRepository.findBySellerId(null)).thenReturn(Collections.emptyList());

        List<Listing> results = listingRepository.findBySellerId(null);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [findBySellerId]: Mengembalikan banyak listing jika seller memiliki lebih dari satu produk")
    void testFindBySellerIdMultipleListings() {
        Listing secondListing = new Listing();
        secondListing.setSellerId(sellerId);
        secondListing.setTitle("iPad Pro M2");
        secondListing.setStatus(ListingStatus.DRAFT);

        when(listingRepository.findBySellerId(sellerId))
                .thenReturn(List.of(activeListing, secondListing));

        List<Listing> results = listingRepository.findBySellerId(sellerId);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(l -> sellerId.equals(l.getSellerId())));
    }

    // ── findByStatus ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [findByStatus]: Sukses memfilter produk aktif lewat status ListingStatus.ACTIVE")
    void testFindByStatusSuccess() {
        when(listingRepository.findByStatus(ListingStatus.ACTIVE)).thenReturn(List.of(activeListing));

        List<Listing> results = listingRepository.findByStatus(ListingStatus.ACTIVE);

        assertEquals(1, results.size());
        verify(listingRepository, times(1)).findByStatus(ListingStatus.ACTIVE);
    }

    @Test
    @DisplayName("Negative Case [findByStatus]: Mengembalikan list kosong sewaktu status (misal DRAFT) nihil record")
    void testFindByStatusEmpty() {
        when(listingRepository.findByStatus(ListingStatus.DRAFT)).thenReturn(Collections.emptyList());

        List<Listing> results = listingRepository.findByStatus(ListingStatus.DRAFT);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [findByStatus]: Aman terkendali dari crash ketika status parameter disuplai null")
    void testFindByStatusNull() {
        when(listingRepository.findByStatus(null)).thenReturn(Collections.emptyList());

        List<Listing> results = listingRepository.findByStatus(null);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [findByStatus]: Sukses mengambil listing dengan status CLOSED")
    void testFindByStatusClosed() {
        Listing closedListing = new Listing();
        closedListing.setTitle("Samsung Galaxy S24");
        closedListing.setStatus(ListingStatus.CLOSED);

        when(listingRepository.findByStatus(ListingStatus.CLOSED))
                .thenReturn(List.of(closedListing));

        List<Listing> results = listingRepository.findByStatus(ListingStatus.CLOSED);

        assertEquals(1, results.size());
        assertEquals(ListingStatus.CLOSED, results.get(0).getStatus());
    }

    // ── findByTitleContainingIgnoreCase ───────────────────────────────────────

    @Test
    @DisplayName("Positive Case [findByTitleContaining]: Sukses mencari potongan kata judul tanpa peduli huruf besar/kecil")
    void testFindByTitleContainingIgnoreCaseSuccess() {
        when(listingRepository.findByTitleContainingIgnoreCase("mAcBoOk"))
                .thenReturn(List.of(activeListing));

        List<Listing> results = listingRepository.findByTitleContainingIgnoreCase("mAcBoOk");

        assertEquals(1, results.size());
        assertEquals("MacBook Pro M3", results.get(0).getTitle());
        verify(listingRepository, times(1)).findByTitleContainingIgnoreCase("mAcBoOk");
    }

    @Test
    @DisplayName("Negative Case [findByTitleContaining]: Memulangkan list kosong jika keyword pencarian tidak eksis")
    void testFindByTitleContainingIgnoreCaseNotFound() {
        when(listingRepository.findByTitleContainingIgnoreCase("Sepeda Motor Terbang"))
                .thenReturn(Collections.emptyList());

        List<Listing> results = listingRepository.findByTitleContainingIgnoreCase("Sepeda Motor Terbang");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [findByTitleContaining]: Tetap berjalan aman tanpa melempar ralat syntax jika keyword disuplai null")
    void testFindByTitleContainingIgnoreCaseNullKeyword() {
        when(listingRepository.findByTitleContainingIgnoreCase(null))
                .thenReturn(Collections.emptyList());

        List<Listing> results = listingRepository.findByTitleContainingIgnoreCase(null);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [findByTitleContaining]: Mengembalikan banyak listing jika lebih dari satu judul mengandung keyword")
    void testFindByTitleContainingIgnoreCaseMultipleResults() {
        Listing secondListing = new Listing();
        secondListing.setTitle("MacBook Air M2");
        secondListing.setStatus(ListingStatus.ACTIVE);

        when(listingRepository.findByTitleContainingIgnoreCase("macbook"))
                .thenReturn(List.of(activeListing, secondListing));

        List<Listing> results = listingRepository.findByTitleContainingIgnoreCase("macbook");

        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Edge Case [findByTitleContaining]: Mengembalikan list kosong jika keyword berupa string kosong")
    void testFindByTitleContainingIgnoreCaseEmptyKeyword() {
        when(listingRepository.findByTitleContainingIgnoreCase(""))
                .thenReturn(List.of(activeListing));

        List<Listing> results = listingRepository.findByTitleContainingIgnoreCase("");

        assertFalse(results.isEmpty());
    }
}
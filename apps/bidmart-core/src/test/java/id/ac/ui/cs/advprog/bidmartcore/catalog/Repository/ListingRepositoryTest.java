package id.ac.ui.cs.advprog.bidmartcore.catalog.repository;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ListingRepositoryTest {

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private TestEntityManager entityManager;
    private Category electronicsCategory;
    private Category clothingCategory;
    private Listing activeListing;
    private UUID sellerId;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        electronicsCategory = new Category();
        electronicsCategory.setName("Elektronik");
        entityManager.persist(electronicsCategory);

        clothingCategory = new Category();
        clothingCategory.setName("Pakaian");
        entityManager.persist(clothingCategory);
        activeListing = new Listing();
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

        entityManager.persist(activeListing);
        entityManager.flush();
    }

    @Test
    @DisplayName("Positive Case [recordNewBid]: Sukses memperbarui nominal bidaan baru")
    void testRecordNewBidSuccess() {
        UUID winnerId = UUID.randomUUID();
        BigDecimal newPrice = new BigDecimal("26000000");

        listingRepository.recordNewBid(activeListing.getId(), newPrice, winnerId);
        entityManager.clear();

        Listing updated = listingRepository.findById(activeListing.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(3, updated.getBidCount(), "bidCount wajib berinkremen naik (+1)");
        assertEquals(newPrice, updated.getCurrentPrice());
        assertEquals(winnerId, updated.getWinnerId());
    }

    @Test
    @DisplayName("Negative Case [recordNewBid]: Tidak merubah record lain jika target ID tidak cocok")
    void testRecordNewBidNotFound() {
        listingRepository.recordNewBid(UUID.randomUUID(), new BigDecimal("99000000"), UUID.randomUUID());
        entityManager.clear();

        Listing original = listingRepository.findById(activeListing.getId()).orElse(null);
        assertNotNull(original);
        assertEquals(2, original.getBidCount(), "Data awal tidak boleh terdistorsi");
    }

    @Test
    @DisplayName("Edge Case [recordNewBid]: Menguji kegunaan COALESCE apabila data awal bidCount bernilai null")
    void testRecordNewBidWhenBidCountIsNull() {
        Listing edgeListing = new Listing();
        edgeListing.setSellerId(sellerId);
        edgeListing.setCategory(electronicsCategory);
        edgeListing.setTitle("iPad Air");
        edgeListing.setDescription("Baru");
        edgeListing.setStartingPrice(new BigDecimal("10000000"));
        edgeListing.setCurrentPrice(new BigDecimal("10000000"));
        edgeListing.setBidCount(null); // State null database
        edgeListing.setStatus(ListingStatus.ACTIVE);

        entityManager.persist(edgeListing);
        entityManager.flush();

        listingRepository.recordNewBid(edgeListing.getId(), new BigDecimal("10500000"), UUID.randomUUID());
        entityManager.clear();

        Listing updated = listingRepository.findById(edgeListing.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(1, updated.getBidCount(), "COALESCE(null, 0) + 1 harus menghasilkan angka 1");
    }

    @Test
    @DisplayName("Positive Case [findByCategoryId]: Sukses mencari daftar produk berdasarkan ID kategori yang cocok")
    void testFindByCategoryIdSuccess() {
        List<Listing> results = listingRepository.findByCategoryId(electronicsCategory.getId());
        assertEquals(1, results.size());
        assertEquals("MacBook Pro M3", results.get(0).getTitle());
    }

    @Test
    @DisplayName("Negative Case [findByCategoryId]: Mengembalikan senarai kosong jika kategori belum diikat produk")
    void testFindByCategoryIdEmpty() {
        List<Listing> results = listingRepository.findByCategoryId(clothingCategory.getId());
        assertTrue(results.isEmpty(), "Wajib memulangkan senarai kosong (clean code)");
    }

    @Test
    @DisplayName("Edge Case [findByCategoryId]: Menangani pencarian parameter categoryId bernilai null secara aman")
    void testFindByCategoryIdNull() {
        List<Listing> results = listingRepository.findByCategoryId(null);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Positive Case [findBySellerId]: Sukses mengumpulkan semua listing kepemilikan seller tertentu")
    void testFindBySellerIdSuccess() {
        List<Listing> results = listingRepository.findBySellerId(sellerId);
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("Negative Case [findBySellerId]: Mengembalikan list kosong jika seller UUID acak tidak memiliki produk")
    void testFindBySellerIdNotFound() {
        List<Listing> results = listingRepository.findBySellerId(UUID.randomUUID());
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [findBySellerId]: Kebal dari ancaman error sewaktu sellerId bernilai null")
    void testFindBySellerIdNull() {
        List<Listing> results = listingRepository.findBySellerId(null);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Positive Case [findByStatus]: Sukses memfilter produk aktif lewat status ListingStatus.ACTIVE")
    void testFindByStatusSuccess() {
        List<Listing> results = listingRepository.findByStatus(ListingStatus.ACTIVE);
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("Negative Case [findByStatus]: Mengembalikan list kosong sewaktu status (misal DRAFT) nihil record")
    void testFindByStatusEmpty() {
        List<Listing> results = listingRepository.findByStatus(ListingStatus.DRAFT);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [findByStatus]: Aman terkendali dari crash ketika status parameter disuplai null")
    void testFindByStatusNull() {
        List<Listing> results = listingRepository.findByStatus(null);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Positive Case [findByTitleContaining]: Sukses mencari potongan kata judul tanpa peduli huruf besar/kecil")
    void testFindByTitleContainingIgnoreCaseSuccess() {
        // Diperbaiki menggunakan List<Listing> yang legal secara kompilasi Java
        List<Listing> results = listingRepository.findByTitleContainingIgnoreCase("mAcBoOk");
        assertEquals(1, results.size());
        assertEquals("MacBook Pro M3", results.get(0).getTitle());
    }

    @Test
    @DisplayName("Negative Case [findByTitleContaining]: Memulangkan list kosong jika keyword pencarian tidak eksis")
    void testFindByTitleContainingIgnoreCaseNotFound() {
        List<Listing> results = listingRepository.findByTitleContainingIgnoreCase("Sepeda Motor Terbang");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [findByTitleContaining]: Tetap berjalan aman tanpa melempar ralat syntax jika keyword disuplai null")
    void testFindByTitleContainingIgnoreCaseNullKeyword() {
        List<Listing> results = listingRepository.findByTitleContainingIgnoreCase(null);
        assertTrue(results.isEmpty());
    }
}
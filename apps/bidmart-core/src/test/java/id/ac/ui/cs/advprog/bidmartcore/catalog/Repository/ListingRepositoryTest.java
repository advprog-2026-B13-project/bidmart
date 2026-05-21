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

    private Category sampleCategory;
    private Listing sampleListing;
    private UUID sellerId;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        sampleCategory = new Category();
        sampleCategory.setName("Gajet");
        entityManager.persist(sampleCategory);
        sampleListing = new Listing();
        sampleListing.setSellerId(sellerId);
        sampleListing.setCategory(sampleCategory);
        sampleListing.setTitle("iPhone 15 Pro Max");
        sampleListing.setDescription("Barang pajakan kilang, kondisi mantap.");
        sampleListing.setStartingPrice(new BigDecimal("15000000"));
        sampleListing.setCurrentPrice(new BigDecimal("15000000"));
        sampleListing.setMinBidIncrement(new BigDecimal("500000"));
        sampleListing.setBidCount(2);
        sampleListing.setStatus(ListingStatus.ACTIVE);
        sampleListing.setStartTime(LocalDateTime.now());
        sampleListing.setEndTime(LocalDateTime.now().plusDays(2));

        entityManager.persist(sampleListing);
        entityManager.flush();
    }

    @Test
    @DisplayName("Positive Case: Sukses memperbarui bidaan baru menggunakan recordNewBid")
    void testRecordNewBidSuccess() {
        UUID winnerId = UUID.randomUUID();
        BigDecimal newPrice = new BigDecimal("16000000");
        listingRepository.recordNewBid(sampleListing.getId(), newPrice, winnerId);
        entityManager.clear();
        Listing updatedListing = listingRepository.findById(sampleListing.getId()).orElse(null);
        assertNotNull(updatedListing);
        assertEquals(3, updatedListing.getBidCount(), "Jumlah bidCount harus bertambah 1 dari nilai awal (2 + 1 = 3)");
        assertEquals(newPrice, updatedListing.getCurrentPrice(), "currentPrice harus diperbarui sesuai dengan nilai bidaan baru");
        assertEquals(winnerId, updatedListing.getWinnerId(), "ID pemenang bidaan terbaru harus tercatat dengan benar");
    }

    @Test
    @DisplayName("Negative Case: Pencarian kata kunci judul lelang yang tidak eksis harus mengembalikan list kosong")
    void testFindByTitleContainingIgnoreCaseNotFound() {
        List<Listings> results = listingRepository.findByTitleContainingIgnoreCase("Sepeda Motor Terbang");
        assertNotNull(results, "Repository tidak boleh mengembalikan nilai null");
        assertTrue(results.isEmpty(), "Senarai hasil pencarian haruslah kosong jika data tidak ditemui");
    }

    @Test
    @DisplayName("Edge Case: Menguji kegunaan COALESCE pada recordNewBid apabila bidCount awal bernilai null")
    void testRecordNewBidWhenBidCountIsNull() {
        Listing edgeListing = new Listing();
        edgeListing.setSellerId(sellerId);
        edgeListing.setCategory(sampleCategory);
        edgeListing.setTitle("PlayStation 5");
        edgeListing.setDescription("Baru gres.");
        edgeListing.setStartingPrice(new BigDecimal("8000000"));
        edgeListing.setCurrentPrice(new BigDecimal("8000000"));
        edgeListing.setBidCount(null);
        edgeListing.setStatus(ListingStatus.ACTIVE);

        entityManager.persist(edgeListing);
        entityManager.flush();

        UUID bidderId = UUID.randomUUID();
        BigDecimal bidAmount = new BigDecimal("8500000");

        listingRepository.recordNewBid(edgeListing.getId(), bidAmount, bidderId);
        entityManager.clear();

        Listing updatedEdgeListing = listingRepository.findById(edgeListing.getId()).orElse(null);
        assertNotNull(updatedEdgeListing);
        assertEquals(1, updatedEdgeListing.getBidCount(), "COALESCE harus mengubah null menjadi 0, sehingga 0 + 1 = 1");
        assertEquals(bidAmount, updatedEdgeListing.getCurrentPrice());
        assertEquals(bidderId, updatedEdgeListing.getWinnerId());
    }
}
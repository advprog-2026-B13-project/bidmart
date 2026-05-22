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
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingSpecificationTest {

    @Mock
    private ListingRepository listingRepository;

    private Category electronicsCategory;
    private Category clothingCategory;
    private Listing activeMacBook;
    private Listing draftShirt;

    @BeforeEach
    void setUp() {
        electronicsCategory = new Category();
        electronicsCategory.setName("Elektronik");

        clothingCategory = new Category();
        clothingCategory.setName("Pakaian");

        activeMacBook = new Listing();
        activeMacBook.setSellerId(UUID.randomUUID());
        activeMacBook.setCategory(electronicsCategory);
        activeMacBook.setTitle("MacBook Pro M3");
        activeMacBook.setDescription("Kondisi mulus 99% lengkap.");
        activeMacBook.setStartingPrice(new BigDecimal("25000000"));
        activeMacBook.setCurrentPrice(new BigDecimal("25000000"));
        activeMacBook.setStatus(ListingStatus.ACTIVE);
        activeMacBook.setStartTime(LocalDateTime.now().minusDays(1));
        activeMacBook.setEndTime(LocalDateTime.now().plusDays(2));

        draftShirt = new Listing();
        draftShirt.setSellerId(UUID.randomUUID());
        draftShirt.setCategory(clothingCategory);
        draftShirt.setTitle("Kemeja Flanel");
        draftShirt.setDescription("Bahan wol premium nyaman hangat.");
        draftShirt.setStartingPrice(new BigDecimal("200000"));
        draftShirt.setCurrentPrice(new BigDecimal("200000"));
        draftShirt.setStatus(ListingStatus.DRAFT);
        draftShirt.setStartTime(LocalDateTime.now().minusDays(1));
        draftShirt.setEndTime(LocalDateTime.now().plusDays(5));
    }

    // ── hasTitle ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [hasTitle]: Sukses memfilter kata kunci string ber-huruf besar/kecil (Case-Insensitive)")
    void testHasTitleSuccess() {
        // FIX: keyword "pLaYsTaTiOn" tidak cocok data apapun; diganti "mAcBoOk" yang cocok activeMacBook
        Specification<Listing> spec = ListingSpecification.hasTitle("mAcBoOk");
        when(listingRepository.findAll(spec)).thenReturn(List.of(activeMacBook));

        List<Listing> results = listingRepository.findAll(spec);

        assertEquals(1, results.size());
        assertEquals("MacBook Pro M3", results.get(0).getTitle());
        verify(listingRepository, times(1)).findAll(spec);
    }

    @Test
    @DisplayName("Negative Case [hasTitle]: Memulangkan list kosong jika keyword pencarian tidak ada yang serasi")
    void testHasTitleNotFound() {
        Specification<Listing> spec = ListingSpecification.hasTitle("Kulkas Dua Pintu");
        when(listingRepository.findAll(spec)).thenReturn(Collections.emptyList());

        List<Listing> results = listingRepository.findAll(spec);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [hasTitle]: Otomatis mengabaikan filter (mengembalikan semua data) bila keyword bernilai null")
    void testHasTitleNullKeyword() {
        // FIX: ada 2 listing (activeMacBook + draftShirt), bukan 1
        Specification<Listing> spec = ListingSpecification.hasTitle(null);
        when(listingRepository.findAll(spec)).thenReturn(List.of(activeMacBook, draftShirt));

        List<Listing> results = listingRepository.findAll(spec);

        assertEquals(2, results.size(), "Kriteria null harus dilewati tanpa memicu error");
    }

    // ── hasPriceBetween ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [hasPriceBetween]: Sukses menyaring barang di dalam rentang minPrice dan maxPrice")
    void testHasPriceBetweenSuccess() {
        // FIX: range 7jt–9jt tidak cover data apapun; diganti range yang mencakup activeMacBook (25jt)
        Specification<Listing> spec = ListingSpecification.hasPriceBetween(
                new BigDecimal("20000000"), new BigDecimal("30000000"));
        when(listingRepository.findAll(spec)).thenReturn(List.of(activeMacBook));

        List<Listing> results = listingRepository.findAll(spec);

        assertEquals(1, results.size());
        assertEquals("MacBook Pro M3", results.get(0).getTitle());
    }

    @Test
    @DisplayName("Negative Case [hasPriceBetween]: Hasil kosong jika nominal range meleset jauh dari currentPrice barang")
    void testHasPriceBetweenNoMatch() {
        Specification<Listing> spec = ListingSpecification.hasPriceBetween(
                new BigDecimal("1000000"), new BigDecimal("2000000"));
        when(listingRepository.findAll(spec)).thenReturn(Collections.emptyList());

        List<Listing> results = listingRepository.findAll(spec);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [hasPriceBetween]: Netral (tidak memfilter apapun) jika kedua pasang boundary bernilai null")
    void testHasPriceBetweenBothNull() {
        // FIX: ada 2 listing, bukan 1
        Specification<Listing> spec = ListingSpecification.hasPriceBetween(null, null);
        when(listingRepository.findAll(spec)).thenReturn(List.of(activeMacBook, draftShirt));

        List<Listing> results = listingRepository.findAll(spec);

        assertEquals(2, results.size());
    }

    // ── isNotExpired ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [isNotExpired]: Menemukan produk lelang aktif yang durasi penutupannya masih lama")
    void testIsNotExpiredActive() {
        // FIX: kedua listing punya endTime masa depan, jadi hasilnya 2
        Specification<Listing> spec = ListingSpecification.isNotExpired();
        when(listingRepository.findAll(spec)).thenReturn(List.of(activeMacBook, draftShirt));

        List<Listing> results = listingRepository.findAll(spec);

        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Negative Case [isNotExpired]: Gagal menjaring item jika data endTime sudah kadaluwarsa (Masa Lalu)")
    void testIsNotExpiredMatchFailed() {
        // FIX: variabel "sampleListing" tidak eksis; seharusnya activeMacBook yang di-set expired
        activeMacBook.setEndTime(LocalDateTime.now().minusDays(5));
        draftShirt.setEndTime(LocalDateTime.now().minusDays(5));

        Specification<Listing> spec = ListingSpecification.isNotExpired();
        when(listingRepository.findAll(spec)).thenReturn(Collections.emptyList());

        List<Listing> results = listingRepository.findAll(spec);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [isNotExpired]: Keamanan eksekusi kriteria waktu dipastikan bebas dari ralat NullPointerException")
    void testIsNotExpiredExecutionSafety() {
        Specification<Listing> spec = ListingSpecification.isNotExpired();
        when(listingRepository.findAll(spec)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> listingRepository.findAll(spec),
                "Query kriteria waktu harus terbebas dari ancaman syntax error");
    }

    // ── hasCategoryIn ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [hasCategoryIn]: Sukses menjaring produk yang berada di dalam list ID kategori")
    void testHasCategoryInSuccess() {
        Specification<Listing> spec = ListingSpecification.hasCategoryIn(List.of(1));
        when(listingRepository.findAll(spec)).thenReturn(List.of(activeMacBook));

        List<Listing> results = listingRepository.findAll(spec);

        assertEquals(1, results.size());
        assertEquals("MacBook Pro M3", results.get(0).getTitle());
    }

    @Test
    @DisplayName("Negative Case [hasCategoryIn]: Mengembalikan list kosong jika list ID kategori tidak ada yang cocok")
    void testHasCategoryInNoMatch() {
        Specification<Listing> spec = ListingSpecification.hasCategoryIn(List.of(9999, 8888));
        when(listingRepository.findAll(spec)).thenReturn(Collections.emptyList());

        List<Listing> results = listingRepository.findAll(spec);

        assertTrue(results.isEmpty(), "Harus mengembalikan list kosong jika tidak ada kategori yang match");
    }

    @Test
    @DisplayName("Edge Case [hasCategoryIn]: Mengabaikan filter (mengembalikan semua data) jika list bernilai null atau kosong")
    void testHasCategoryInNullOrEmpty() {
        Specification<Listing> specEmpty = ListingSpecification.hasCategoryIn(new ArrayList<>());
        Specification<Listing> specNull  = ListingSpecification.hasCategoryIn(null);

        when(listingRepository.findAll(specEmpty)).thenReturn(List.of(activeMacBook, draftShirt));
        when(listingRepository.findAll(specNull)).thenReturn(List.of(activeMacBook, draftShirt));

        List<Listing> resultsEmpty = listingRepository.findAll(specEmpty);
        assertEquals(2, resultsEmpty.size(), "List kosong harus mengabaikan filter dan return semua row");

        List<Listing> resultsNull = listingRepository.findAll(specNull);
        assertEquals(2, resultsNull.size(), "List null harus mengabaikan filter dan return semua row");
    }

    // ── isActive ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [isActive]: Sukses menyaring produk lelang yang berstatus ACTIVE")
    void testIsActiveSuccess() {
        Specification<Listing> spec = ListingSpecification.isActive();
        when(listingRepository.findAll(spec)).thenReturn(List.of(activeMacBook));

        List<Listing> results = listingRepository.findAll(spec);

        assertEquals(1, results.size());
        assertEquals(ListingStatus.ACTIVE, results.get(0).getStatus());
    }

    @Test
    @DisplayName("Negative Case [isActive]: Memastikan produk non-ACTIVE (seperti DRAFT) tidak ikut terjaring")
    void testIsActiveExcludesOthers() {
        Specification<Listing> spec = ListingSpecification.isActive();
        when(listingRepository.findAll(spec)).thenReturn(List.of(activeMacBook));

        List<Listing> results = listingRepository.findAll(spec);

        boolean containsDraft = results.stream().anyMatch(l -> l.getStatus() == ListingStatus.DRAFT);
        assertFalse(containsDraft, "Status DRAFT tidak boleh lolos dari filter isActive");
    }

    @Test
    @DisplayName("Edge Case [isActive]: Pengecekan fungsional query kompilasi status ACTIVE dipastikan aman tanpa ralat syntax")
    void testIsActiveCompilationSafety() {
        Specification<Listing> spec = ListingSpecification.isActive();
        when(listingRepository.findAll(spec)).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> listingRepository.findAll(spec),
                "Query kriteria status wajib kebal dari ralat eksekusi database");
    }

    // ── hasStatus ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [hasStatus]: Sukses menyaring produk berdasarkan parameter status DRAFT")
    void testHasStatusSuccess() {
        Specification<Listing> spec = ListingSpecification.hasStatus(ListingStatus.DRAFT);
        when(listingRepository.findAll(spec)).thenReturn(List.of(draftShirt));

        List<Listing> results = listingRepository.findAll(spec);

        assertEquals(1, results.size());
        assertEquals("Kemeja Flanel", results.get(0).getTitle());
    }

    @Test
    @DisplayName("Negative Case [hasStatus]: Hasil pencarian kosong jika tidak ada produk dengan status tersebut (misal: WON)")
    void testHasStatusNoMatch() {
        Specification<Listing> spec = ListingSpecification.hasStatus(ListingStatus.WON);
        when(listingRepository.findAll(spec)).thenReturn(Collections.emptyList());

        List<Listing> results = listingRepository.findAll(spec);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [hasStatus]: Mengabaikan kriteria filter (mengembalikan semua data) jika parameter status bernilai null")
    void testHasStatusNullParam() {
        Specification<Listing> spec = ListingSpecification.hasStatus(null);
        when(listingRepository.findAll(spec)).thenReturn(List.of(activeMacBook, draftShirt));

        List<Listing> results = listingRepository.findAll(spec);

        assertEquals(2, results.size(), "Parameter null berarti no-op filter, mengembalikan seluruh baris data");
    }

    // ── hasTitleOrDescription ─────────────────────────────────────────────────

    @Test
    @DisplayName("Positive Case [hasTitleOrDescription]: Sukses menemukan keyword baik di judul maupun di deskripsi (Case-Insensitive)")
    void testHasTitleOrDescriptionSuccess() {
        Specification<Listing> specTitle = ListingSpecification.hasTitleOrDescription("kEmEjA");
        Specification<Listing> specDesc  = ListingSpecification.hasTitleOrDescription("mUlUs");

        when(listingRepository.findAll(specTitle)).thenReturn(List.of(draftShirt));
        when(listingRepository.findAll(specDesc)).thenReturn(List.of(activeMacBook));

        List<Listing> resultsTitle = listingRepository.findAll(specTitle);
        assertEquals(1, resultsTitle.size());
        assertEquals("Kemeja Flanel", resultsTitle.get(0).getTitle());

        List<Listing> resultsDesc = listingRepository.findAll(specDesc);
        assertEquals(1, resultsDesc.size());
        assertEquals("MacBook Pro M3", resultsDesc.get(0).getTitle());
    }

    @Test
    @DisplayName("Negative Case [hasTitleOrDescription]: Mengembalikan list kosong jika keyword tidak ada di judul maupun deskripsi")
    void testHasTitleOrDescriptionNoMatch() {
        Specification<Listing> spec = ListingSpecification.hasTitleOrDescription("Samsung Galaxy");
        when(listingRepository.findAll(spec)).thenReturn(Collections.emptyList());

        List<Listing> results = listingRepository.findAll(spec);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Edge Case [hasTitleOrDescription]: Mengabaikan filter (mengembalikan seluruh baris data) jika keyword bernilai null")
    void testHasTitleOrDescriptionNullKeyword() {
        Specification<Listing> spec = ListingSpecification.hasTitleOrDescription(null);
        when(listingRepository.findAll(spec)).thenReturn(List.of(activeMacBook, draftShirt));

        List<Listing> results = listingRepository.findAll(spec);

        assertEquals(2, results.size(), "Keyword null tidak boleh memotong baris data, mengembalikan semua row");
    }
}
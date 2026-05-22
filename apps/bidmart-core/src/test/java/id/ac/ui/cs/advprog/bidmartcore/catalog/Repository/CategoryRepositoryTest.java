package id.ac.ui.cs.advprog.bidmartcore.catalog.repository;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Category rootCategory1;
    private Category rootCategory2;
    private Category subCategory1;

    @BeforeEach
    void setUp() {
        rootCategory1 = new Category();
        rootCategory1.setName("Elektronik");

        rootCategory2 = new Category();
        rootCategory2.setName("Pakaian");

        subCategory1 = new Category();
        subCategory1.setName("Handphone");
        subCategory1.setParentCategory(rootCategory1);

        entityManager.persist(rootCategory1);
        entityManager.persist(rootCategory2);
        entityManager.persist(subCategory1);
        entityManager.flush();
    }

    @Test
    @DisplayName("Positive Case: Sukses mencari semua kategori utama (parent category bermutu null)")
    void testFindByParentCategoryIsNullSuccess() {
        List<Category> roots = categoryRepository.findByParentCategoryIsNull();
        assertNotNull(roots, "Hasil tidak boleh bernilai null");
        assertEquals(2, roots.size(), "Harus mengembalikan tepat 2 akar kategori");
        assertTrue(roots.stream().anyMatch(c -> c.getName().equals("Elektronik")));
        assertTrue(roots.stream().anyMatch(c -> c.getName().equals("Pakaian")));
        assertFalse(roots.stream().anyMatch(c -> c.getName().equals("Handphone")), "Sub-kategori tidak boleh dimasukkan sebagai akar");
    }

    @Test
    @DisplayName("Negative Case: Mencari nama kategori yang tidak wujud harus mengembalikan Optional.empty")
    void testFindByNameNotFoundReturnsEmptyOptional() {
        Optional<Category> found = categoryRepository.findByName("Buku Rujukan Adpro Lama");
        assertNotNull(found);
        assertTrue(found.isEmpty(), "Harus mengembalikan Optional yang kosong jika entitas tidak ditemui");
    }

    @Test
    @DisplayName("Edge Case: Mencari anak kategori daripada parent yang tidak mempunyai anak harus menghasilkan list kosong (bukan null)")
    void testFindByParentCategoryIdWithNoChildrenReturnsEmptyList() {
        List<Category> children = categoryRepository.findByParentCategoryId(rootCategory2.getId());
        assertNotNull(children, "Repository wajib mengembalikan koleksi list kosong, bukannya null");
        assertTrue(children.isEmpty(), "Koleksi list sub-categories mestilah kosong bagi parent yang tiada anak");
    }
}
package id.ac.ui.cs.advprog.bidmartcore.catalog.repository;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryRepositoryTest {

    @Mock
    private CategoryRepository categoryRepository;

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
    }

    @Test
    @DisplayName("Positive Case: Sukses mencari semua kategori utama (parent category bermutu null)")
    void testFindByParentCategoryIsNullSuccess() {
        when(categoryRepository.findByParentCategoryIsNull())
                .thenReturn(List.of(rootCategory1, rootCategory2));

        List<Category> roots = categoryRepository.findByParentCategoryIsNull();

        assertNotNull(roots, "Hasil tidak boleh bernilai null");
        assertEquals(2, roots.size(), "Harus mengembalikan tepat 2 akar kategori");
        assertTrue(roots.stream().anyMatch(c -> c.getName().equals("Elektronik")));
        assertTrue(roots.stream().anyMatch(c -> c.getName().equals("Pakaian")));
        assertFalse(roots.stream().anyMatch(c -> c.getName().equals("Handphone")),
                "Sub-kategori tidak boleh dimasukkan sebagai akar");

        verify(categoryRepository, times(1)).findByParentCategoryIsNull();
    }

    @Test
    @DisplayName("Negative Case: Mencari nama kategori yang tidak wujud harus mengembalikan Optional.empty")
    void testFindByNameNotFoundReturnsEmptyOptional() {
        String nonExistentName = "Buku Rujukan Adpro Lama";
        when(categoryRepository.findByName(nonExistentName))
                .thenReturn(Optional.empty());

        Optional<Category> found = categoryRepository.findByName(nonExistentName);

        assertNotNull(found);
        assertTrue(found.isEmpty(), "Harus mengembalikan Optional yang kosong jika entitas tidak ditemui");

        verify(categoryRepository, times(1)).findByName(nonExistentName);
    }

    @Test
    @DisplayName("Edge Case: Mencari anak kategori daripada parent yang tidak mempunyai anak harus menghasilkan list kosong (bukan null)")
    void testFindByParentCategoryIdWithNoChildrenReturnsEmptyList() {
        Integer parentId = 999;
        when(categoryRepository.findByParentCategoryId(parentId))
                .thenReturn(Collections.emptyList());

        List<Category> children = categoryRepository.findByParentCategoryId(parentId);

        assertNotNull(children, "Repository wajib mengembalikan koleksi list kosong, bukannya null");
        assertTrue(children.isEmpty(), "Koleksi list sub-categories mestilah kosong bagi parent yang tiada anak");

        verify(categoryRepository, times(1)).findByParentCategoryId(parentId);
    }
}
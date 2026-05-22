package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category sampleCategory;
    private Integer categoryId;

    @BeforeEach
    void setUp() {
        categoryId = 10;
        sampleCategory = new Category();
        sampleCategory.setId(categoryId);
        sampleCategory.setName("Elektronik");
        sampleCategory.setSubCategories(new ArrayList<>());
    }

    @Test
    @DisplayName("Positive Case [getCategoryById]: Sukses menemukan kategori berdasarkan ID")
    void testGetCategoryByIdSuccess() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));
        Category result = categoryService.getCategoryById(categoryId);
        assertNotNull(result);
        assertEquals(categoryId, result.getId());
        assertEquals("Elektronik", result.getName());
        verify(categoryRepository, times(1)).findById(categoryId);
    }

    @Test
    @DisplayName("Negative Case [getCategoryById]: Melempar IllegalArgumentException jika ID tidak terdaftar")
    void testGetCategoryByIdNotFoundThrowsException() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            categoryService.getCategoryById(categoryId);
        });

        assertEquals("Kategori dengan ID " + categoryId + " tidak ditemukan", exception.getMessage());
        verify(categoryRepository, times(1)).findById(categoryId);
    }

    @Test
    @DisplayName("Edge Case [getCategoryById]: Menangani pencarian dengan ID bernilai ekstrem/negatif secara aman")
    void testGetCategoryByIdWithNegativeIdThrowsException() {
        Integer negativeId = -999;
        when(categoryRepository.findById(negativeId)).thenReturn(Optional.empty());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            categoryService.getCategoryById(negativeId);
        });

        assertTrue(exception.getMessage().contains("tidak ditemukan"));
        verify(categoryRepository, times(1)).findById(negativeId);
    }

    @Test
    @DisplayName("Positive Case [updateCategory]: Sukses memperbarui nama dan parent category")
    void testUpdateCategorySuccess() {
        Category parentCategory = new Category();
        parentCategory.setId(20);
        parentCategory.setName("Perabotan");
        Category updateDetails = new Category();
        updateDetails.setName("Gadget");
        updateDetails.setParentCategory(parentCategory);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Category result = categoryService.updateCategory(categoryId, updateDetails);
        assertNotNull(result);
        assertEquals("Gadget", result.getName(), "Nama kategori harus berhasil diperbarui");
        assertEquals(parentCategory, result.getParentCategory(), "Hubungan parent category harus berhasil ditautkan");
        verify(categoryRepository, times(1)).save(sampleCategory);
    }

    @Test
    @DisplayName("Negative Case [updateCategory]: Menolak update jika kategori mencoba menjadi parent dirinya sendiri")
    void testUpdateCategorySameParentThrowsException() {
        Category invalidParent = new Category();
        invalidParent.setId(categoryId);
        Category updateDetails = new Category();
        updateDetails.setName("Elektronik Rusak");
        updateDetails.setParentCategory(invalidParent);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            categoryService.updateCategory(categoryId, updateDetails);
        });
        assertEquals("Validasi Gagal: Kategori tidak boleh menjadi parent dari dirinya sendiri.", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("Edge Case [updateCategory]: Sukses mengosongkan parent category (mengubah sub-kategori menjadi root)")
    void testUpdateCategoryWithNullParentSuccess() {
        Category initialParent = new Category();
        initialParent.setId(50);
        sampleCategory.setParentCategory(initialParent);

        Category updateDetails = new Category();
        updateDetails.setName("Elektronik Mandiri");
        updateDetails.setParentCategory(null);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Category result = categoryService.updateCategory(categoryId, updateDetails);
        assertNotNull(result);
        assertNull(result.getParentCategory(), "Parent category harus berhasil dibersihkan menjadi null");
        verify(categoryRepository, times(1)).save(sampleCategory);
    }

    @Test
    @DisplayName("Positive Case [deleteCategory]: Sukses menghapus kategori yang tidak memiliki anak/sub-categories")
    void testDeleteCategorySuccess() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));
        categoryService.deleteCategory(categoryId);
        verify(categoryRepository, times(1)).delete(sampleCategory);
    }

    @Test
    @DisplayName("Negative Case [deleteCategory]: Melempar IllegalStateException jika kategori masih memiliki sub-categories")
    void testDeleteCategoryWithSubCategoriesThrowsException() {
        Category childCategory = new Category();
        childCategory.setId(11);
        sampleCategory.getSubCategories().add(childCategory);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            categoryService.deleteCategory(categoryId);
        });
        assertEquals("Kategori tidak bisa dihapus karena masih memiliki sub-kategori.", exception.getMessage());
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("Edge Case [deleteCategory]: Sukses menghapus kategori jika properti list subCategories bernilai null")
    void testDeleteCategoryWithNullSubCategoriesListSuccess() {
        sampleCategory.setSubCategories(null);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));
        categoryService.deleteCategory(categoryId);
        verify(categoryRepository, times(1)).delete(sampleCategory);
    }
}
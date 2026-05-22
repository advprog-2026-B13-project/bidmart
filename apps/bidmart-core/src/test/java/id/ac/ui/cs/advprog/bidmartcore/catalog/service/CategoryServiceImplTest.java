package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.CategoryCreateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.CategoryResponse;
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
        CategoryResponse result = categoryService.getCategoryById(categoryId);

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

        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Gadget");
        request.setParentId(20);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.findById(20)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse result = categoryService.updateCategory(categoryId, request);

        assertNotNull(result);
        assertEquals("Gadget", result.getName(), "Nama kategori harus berhasil diperbarui");
        verify(categoryRepository, times(1)).save(sampleCategory);
    }

    @Test
    @DisplayName("Edge Case [updateCategory]: Sukses mengosongkan parent category (mengubah sub-kategori menjadi root)")
    void testUpdateCategoryWithNullParentSuccess() {
        Category initialParent = new Category();
        initialParent.setId(50);
        sampleCategory.setParentCategory(initialParent);

        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Elektronik Mandiri");
        request.setParentId(null);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse result = categoryService.updateCategory(categoryId, request);

        assertNotNull(result);
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

    @Test
    @DisplayName("Positive Case [createCategory]: Sukses menyimpan kategori baru tingkat akar (root)")
    void testCreateCategorySuccess() {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Buku");

        Category savedCategory = new Category();
        savedCategory.setId(1);
        savedCategory.setName("Buku");

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponse result = categoryService.createCategory(request);

        assertNotNull(result);
        assertEquals("Buku", result.getName());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Negative Case [createCategory]: Meneruskan exception jika database gagal melakukan persist data")
    void testCreateCategoryThrowsExceptionOnRepositoryError() {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Data Invalid");

        when(categoryRepository.save(any(Category.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> {
            categoryService.createCategory(request);
        });

        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Edge Case [createCategory]: Sukses menyimpan kategori yang sudah mengikat hubungan parent")
    void testCreateCategoryWithParentSuccess() {
        Category parent = new Category();
        parent.setId(1);

        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Kemeja");
        request.setParentId(1);

        Category savedCategory = new Category();
        savedCategory.setId(2);
        savedCategory.setName("Kemeja");
        savedCategory.setParentCategory(parent);

        when(categoryRepository.findById(1)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponse result = categoryService.createCategory(request);

        assertNotNull(result);
        assertEquals("Kemeja", result.getName());
        verify(categoryRepository, times(1)).findById(1);
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("Positive Case [getMainCategories]: Sukses mengambil seluruh daftar kategori utama (root)")
    void testGetMainCategoriesSuccess() {
        List<Category> mockRoots = List.of(sampleCategory);
        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(mockRoots);

        List<CategoryResponse> results = categoryService.getMainCategories();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Elektronik", results.get(0).getName());
        verify(categoryRepository, times(1)).findByParentCategoryIsNull();
    }

    @Test
    @DisplayName("Negative Case [getMainCategories]: Mengembalikan list kosong jika belum ada kategori utama di database")
    void testGetMainCategoriesEmpty() {
        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(List.of());

        List<CategoryResponse> results = categoryService.getMainCategories();

        assertNotNull(results);
        assertTrue(results.isEmpty(), "Harus mengembalikan koleksi list kosong, bukan null");
        verify(categoryRepository, times(1)).findByParentCategoryIsNull();
    }

    @Test
    @DisplayName("Edge Case [getMainCategories]: Menangani situasi aman saat terjadi null pointer pada stream data database")
    void testGetMainCategoriesReturnsNullSafety() {
        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(null);

        // Memastikan sistem meneruskan NullPointerException secara natif karena stream tidak bisa handle null List dari DB
        assertThrows(NullPointerException.class, () -> {
            categoryService.getMainCategories();
        });

        verify(categoryRepository, times(1)).findByParentCategoryIsNull();
    }

    @Test
    @DisplayName("Positive Case [getSubCategories]: Sukses mengambil semua sub-kategori dari parent ID yang valid")
    void testGetSubCategoriesSuccess() {
        Category subCategory = new Category();
        subCategory.setId(11);
        subCategory.setName("Laptop");

        when(categoryRepository.findByParentCategoryId(categoryId)).thenReturn(List.of(subCategory));

        List<CategoryResponse> results = categoryService.getSubCategories(categoryId);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Laptop", results.get(0).getName());
        verify(categoryRepository, times(1)).findByParentCategoryId(categoryId);
    }

    @Test
    @DisplayName("Negative Case [getSubCategories]: Mengembalikan list kosong jika parent ID tersebut tidak memiliki anak kategori")
    void testGetSubCategoriesEmpty() {
        when(categoryRepository.findByParentCategoryId(categoryId)).thenReturn(List.of());

        List<CategoryResponse> results = categoryService.getSubCategories(categoryId);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(categoryRepository, times(1)).findByParentCategoryId(categoryId);
    }

    @Test
    @DisplayName("Edge Case [getSubCategories]: Berjalan aman saat mengecek sub-kategori dengan parameter ID ekstrem/negatif")
    void testGetSubCategoriesWithNegativeId() {
        Integer negativeId = -100;
        when(categoryRepository.findByParentCategoryId(negativeId)).thenReturn(List.of());

        List<CategoryResponse> results = categoryService.getSubCategories(negativeId);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(categoryRepository, times(1)).findByParentCategoryId(negativeId);
    }
}
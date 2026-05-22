package id.ac.ui.cs.advprog.bidmartcore.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category();
        sampleCategory.setId(1);
        sampleCategory.setName("Elektronik");
        sampleCategory.setSubCategories(new ArrayList<>());
    }

    @Test
    @DisplayName("Positive Case [createCategory]: Admin sukses membuat kategori baru dengan payload valid")
    void testCreateCategorySuccess() throws Exception {
        String jsonRequest = "{\"name\":\"Elektronik\",\"parentId\":null}";
        when(categoryService.createCategory(any(Category.class))).thenReturn(sampleCategory);

        mockMvc.perform(post("/api/catalog/categories")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Elektronik"));
    }

    @Test
    @DisplayName("Negative Case [createCategory]: Menolak pembuatan kategori jika dilakukan oleh USER")
    void testCreateCategoryAccessDenied() throws Exception {
        String jsonRequest = "{\"name\":\"Buku\",\"parentId\":null}";

        mockMvc.perform(post("/api/catalog/categories")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().is5xxClientError());

        verify(categoryService, never()).createCategory(any());
    }

    @Test
    @DisplayName("Edge Case [createCategory]: Gagal membuat kategori jika parentId DTO tidak valid")
    void testCreateCategoryParentNotFound() throws Exception {
        String jsonRequest = "{\"name\":\"Laptop\",\"parentId\":999}";
        when(categoryService.getCategoryById(999)).thenThrow(new IllegalArgumentException("Kategori tidak ditemukan"));

        mockMvc.perform(post("/api/catalog/categories")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().is5xxClientError());
    }

    @Test
    @DisplayName("Positive Case [updateCategory]: Admin sukses memperbarui data nama kategori")
    void testUpdateCategorySuccess() throws Exception {
        String jsonRequest = "{\"name\":\"Elektronik Terupdate\"}";
        sampleCategory.setName("Elektronik Terupdate");
        when(categoryService.updateCategory(eq(1), any(Category.class))).thenReturn(sampleCategory);

        mockMvc.perform(put("/api/catalog/categories/{id}", 1)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Elektronik Terupdate"));
    }

    @Test
    @DisplayName("Negative Case [updateCategory]: Menolak perubahan data jika dilakukan oleh role USER")
    void testUpdateCategoryAccessDenied() throws Exception {
        String jsonRequest = "{\"name\":\"Gadget\"}";

        mockMvc.perform(put("/api/catalog/categories/{id}", 1)
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().is5xxClientError());
    }

    @Test
    @DisplayName("Edge Case [updateCategory]: Gagal jika pembaharuan dari service memicu loop hierarki")
    void testUpdateCategoryHierarchyLoop() throws Exception {
        String jsonRequest = "{\"name\":\"Loop Kategori\"}";
        when(categoryService.updateCategory(eq(1), any(Category.class)))
                .thenThrow(new IllegalArgumentException("Kategori tidak boleh menjadi parent dari dirinya sendiri."));

        mockMvc.perform(put("/api/catalog/categories/{id}", 1)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().is5xxClientError());
    }

    @Test
    @DisplayName("Positive Case [deleteCategory]: Admin sukses menghapus kategori kosong berdasarkan ID")
    void testDeleteCategorySuccess() throws Exception {
        doNothing().when(categoryService).deleteCategory(1);

        mockMvc.perform(delete("/api/catalog/categories/{id}", 1)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNoContent());

        verify(categoryService, times(1)).deleteCategory(1);
    }

    @Test
    @DisplayName("Negative Case [deleteCategory]: Menolak penghapusan kategori jika role panggil bukan ADMIN")
    void testDeleteCategoryAccessDenied() throws Exception {
        mockMvc.perform(delete("/api/catalog/categories/{id}", 1)
                        .header("X-User-Role", "USER"))
                .andExpect(status().is5xxClientError());
    }

    @Test
    @DisplayName("Edge Case [deleteCategory]: Menolak hapus jika kategori masih memiliki sub-kategori aktif")
    void testDeleteCategoryWithSubCategoriesConflict() throws Exception {
        doThrow(new IllegalStateException("Kategori tidak bisa dihapus karena masih memiliki sub-kategori."))
                .when(categoryService).deleteCategory(1);

        mockMvc.perform(delete("/api/catalog/categories/{id}", 1)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().is5xxClientError());
    }

    @Test
    @DisplayName("Positive Case [getCategoryById]: Sukses mengambil detail kategori berdasarkan ID yang eksis")
    void testGetCategoryByIdSuccess() throws Exception {
        when(categoryService.getCategoryById(1)).thenReturn(sampleCategory);

        mockMvc.perform(get("/api/catalog/categories/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Elektronik"));
    }

    @Test
    @DisplayName("Negative Case [getCategoryById]: Mengembalikan ralat status 5xx jika ID tidak ditemukan")
    void testGetCategoryByIdNotFound() throws Exception {
        when(categoryService.getCategoryById(404)).thenThrow(new IllegalArgumentException("Kategori tidak ditemukan"));

        mockMvc.perform(get("/api/catalog/categories/{id}", 404))
                .andExpect(status().is5xxClientError());
    }

    @Test
    @DisplayName("Edge Case [getCategoryById]: Menangani request pencarian dengan parameter ID ekstrem/negatif")
    void testGetCategoryByIdNegativeId() throws Exception {
        when(categoryService.getCategoryById(-1)).thenThrow(new IllegalArgumentException("Kategori tidak ditemukan"));

        mockMvc.perform(get("/api/catalog/categories/{id}", -1))
                .andExpect(status().is5xxClientError());
    }

    @Test
    @DisplayName("Positive Case [getMainCategories]: Sukses mengambil seluruh daftar kategori utama")
    void testGetMainCategoriesSuccess() throws Exception {
        when(categoryService.getMainCategories()).thenReturn(List.of(sampleCategory));

        mockMvc.perform(get("/api/catalog/categories/main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Elektronik"));

        verify(categoryService, times(1)).getMainCategories();
    }

    @Test
    @DisplayName("Negative Case [getMainCategories]: Sukses mengembalikan array kosong jika belum ada data")
    void testGetMainCategoriesEmpty() throws Exception {
        when(categoryService.getMainCategories()).thenReturn(List.of());

        mockMvc.perform(get("/api/catalog/categories/main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Edge Case [getMainCategories]: Menangani kegagalan internal server pangkalan data secara aman")
    void testGetMainCategoriesServerError() throws Exception {
        when(categoryService.getMainCategories()).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/catalog/categories/main"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Positive Case [getSubCategories]: Sukses mengambil sub-kategori dari parent ID yang valid")
    void testGetSubCategoriesSuccess() throws Exception {
        Category subCategory = new Category();
        subCategory.setId(2);
        subCategory.setName("Smartphone");

        when(categoryService.getSubCategories(1)).thenReturn(List.of(subCategory));

        mockMvc.perform(get("/api/catalog/categories/sub/{parentId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Smartphone"));

        verify(categoryService, times(1)).getSubCategories(1);
    }

    @Test
    @DisplayName("Negative Case [getSubCategories]: Mengembalikan array kosong jika parent tidak memiliki sub-kategori")
    void testGetSubCategoriesEmpty() throws Exception {
        when(categoryService.getSubCategories(1)).thenReturn(List.of());

        mockMvc.perform(get("/api/catalog/categories/sub/{parentId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Edge Case [getSubCategories]: Menangani parameter parent ID bernilai negatif secara aman")
    void testGetSubCategoriesNegativeId() throws Exception {
        when(categoryService.getSubCategories(-99)).thenReturn(List.of());

        mockMvc.perform(get("/api/catalog/categories/sub/{parentId}", -99))
                .andExpect(status().isOk());
    }
}
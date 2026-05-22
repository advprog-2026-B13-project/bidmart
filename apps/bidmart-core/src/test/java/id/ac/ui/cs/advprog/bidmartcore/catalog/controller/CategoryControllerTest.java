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
    @DisplayName("Positive Case [createCategory]: Sukses membuat kategori baru tingkat akar (root)")
    void testCreateCategorySuccess() throws Exception {
        String jsonRequest = "{\"name\":\"Elektronik\",\"parentId\":null}";
        when(categoryService.createCategory(any(Category.class))).thenReturn(sampleCategory);
        mockMvc.perform(post("/api/catalog/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Elektronik"));

        verify(categoryService, times(1)).createCategory(any(Category.class));
    }

    @Test
    @DisplayName("Negative Case [createCategory]: Gagal jika nama kategori kosong (Validation Error)")
    void testCreateCategoryValidationError() throws Exception {
        String invalidJsonRequest = "{\"name\":\"\",\"parentId\":null}";
        mockMvc.perform(post("/api/catalog/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJsonRequest))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory(any(Category.class));
    }

    @Test
    @DisplayName("Edge Case [createCategory]: Gagal membuat kategori jika parentId tidak eksis di sistem")
    void testCreateCategoryParentNotFound() throws Exception {
        String jsonRequestWithParent = "{\"name\":\"Smartphone\",\"parentId\":99}";
        when(categoryService.getCategoryById(99)).thenThrow(new IllegalArgumentException("Kategori dengan ID 99 tidak ditemukan"));
        mockMvc.perform(post("/api/catalog/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequestWithParent))
                .andExpect(status().is4xxClientError());

        verify(categoryService, times(1)).getCategoryById(99);
        verify(categoryService, never()).createCategory(any(Category.class));
    }

    @Test
    @DisplayName("Positive Case [updateCategory]: Admin berhasil memperbarui data nama kategori")
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

        verify(categoryService, times(1)).updateCategory(eq(1), any(Category.class));
    }

    @Test
    @DisplayName("Negative Case [updateCategory]: Menolak akses jika pengguna biasa (USER) mencoba mengubah data")
    void testUpdateCategoryAccessDenied() throws Exception {
        String jsonRequest = "{\"name\":\"Gadget\"}";
        mockMvc.perform(put("/api/catalog/categories/{id}", 1)
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().is4xxClientError());

        verify(categoryService, never()).updateCategory(anyInt(), any(Category.class));
    }

    @Test
    @DisplayName("Edge Case [updateCategory]: Gagal jika pembaharuan memicu loop hierarki kategori")
    void testUpdateCategoryHierarchyLoop() throws Exception {
        String jsonRequest = "{\"name\":\"Elektronik Loop\"}";
        when(categoryService.updateCategory(eq(1), any(Category.class)))
                .thenThrow(new IllegalArgumentException("Validasi Gagal: Kategori tidak boleh menjadi parent dari dirinya sendiri."));
        mockMvc.perform(put("/api/catalog/categories/{id}", 1)
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest());

        verify(categoryService, times(1)).updateCategory(eq(1), any(Category.class));
    }

    @Test
    @DisplayName("Positive Case [deleteCategory]: Admin sukses menghapus kategori kosong via ID")
    void testDeleteCategorySuccess() throws Exception {
        doNothing().when(categoryService).deleteCategory(1);
        mockMvc.perform(delete("/api/catalog/categories/{id}", 1)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNoContent());

        verify(categoryService, times(1)).deleteCategory(1);
    }

    @Test
    @DisplayName("Negative Case [deleteCategory]: Gagalkan aksi penghapusan jika header role kosong/USER")
    void testDeleteCategoryAccessDenied() throws Exception {
        mockMvc.perform(delete("/api/catalog/categories/{id}", 1)
                        .header("X-User-Role", "USER"))
                .andExpect(status().is4xxClientError());

        verify(categoryService, never()).deleteCategory(anyInt());
    }

    @Test
    @DisplayName("Edge Case [deleteCategory]: Menolak hapus jika kategori masih mengikat anak (sub-kategori)")
    void testDeleteCategoryWithSubCategoriesConflict() throws Exception {
        doThrow(new IllegalStateException("Kategori tidak bisa dihapus karena masih memiliki sub-kategori."))
                .when(categoryService).deleteCategory(1);

        mockMvc.perform(delete("/api/catalog/categories/{id}", 1)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().is4xxClientError());
        verify(categoryService, times(1)).deleteCategory(1);
    }
}
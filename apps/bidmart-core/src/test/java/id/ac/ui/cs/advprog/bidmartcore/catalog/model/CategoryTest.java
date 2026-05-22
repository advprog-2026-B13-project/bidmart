package id.ac.ui.cs.advprog.bidmartcore.catalog.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    private Category mainCategory;
    private Category subCategory;

    @BeforeEach
    void setUp() {
        mainCategory = new Category();
        mainCategory.setId(1);
        mainCategory.setName("Elektronik");
        mainCategory.setSubCategories(new ArrayList<>());

        subCategory = new Category();
        subCategory.setId(2);
        subCategory.setName("Handphone");
        subCategory.setSubCategories(new ArrayList<>());
    }

    @Test
    @DisplayName("Positive Case: Sukses membangun hubungan hierarki parent-child antar kategori")
    void testCategoryParentChildRelationshipSuccess() {
        subCategory.setParentCategory(mainCategory);
        mainCategory.getSubCategories().add(subCategory);
        assertNotNull(subCategory.getParentCategory(), "Sub-kategori harus memiliki referensi ke parent-nya");
        assertEquals("Elektronik", subCategory.getParentCategory().getName(), "Nama parent category harus sesuai");
        assertEquals(1, mainCategory.getSubCategories().size(), "Parent category harus mencatat sub-kategori di dalam list-nya");
        assertEquals("Handphone", mainCategory.getSubCategories().get(0).getName(), "Isi dari list sub-categories harus sesuai");
    }

    @Test
    @DisplayName("Negative Case: Verifikasi category terisolasi tanpa parent (Root Category)")
    void testRootCategoryHasNullParent() {
        assertNull(mainCategory.getParentCategory(), "Kategori utama (root) harus memiliki parentCategory bernilai null");
        assertTrue(mainCategory.getSubCategories().isEmpty(), "Kategori baru secara default list sub-kategorinya kosong");
    }

    @Test
    @DisplayName("Edge Case: Memastikan integritas data mutasi nama dan list sub-categories yang independen")
    void testCategoryDataIntegrityAndListMutation() {
        Category thirdCategory = new Category();
        thirdCategory.setId(3);
        thirdCategory.setName("Laptop");
        thirdCategory.setSubCategories(null);
        assertNull(thirdCategory.getSubCategories(), "State awal sub-categories bisa bernilai null sebelum di-fetch / diinisialisasi");
        thirdCategory.setName("Laptop & PC");
        assertEquals("Laptop & PC", thirdCategory.getName(), "Nama kategori harus berhasil diperbarui tanpa mengganggu properti lainnya");
    }
}
package id.ac.ui.cs.advprog.bidmartcore.catalog.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.CategoryService;
import lombok.RequiredArgsConstructor;

@RestController("catalogCategoryController")
@RequestMapping("/api/catalog/categories")
@RequiredArgsConstructor
public class CategoryController {

    private static final String ADMIN_ROLE = ADMIN_ROLE;

    private final CategoryService categoryService;

    @GetMapping("/main")
    public ResponseEntity<List<Category>> getMainCategories() {
        return ResponseEntity.ok(categoryService.getMainCategories());
    }

    @GetMapping("/sub/{parentId}")
    public ResponseEntity<List<Category>> getSubCategories(@PathVariable Integer parentId) {
        return ResponseEntity.ok(categoryService.getSubCategories(parentId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role,
            @RequestBody Category category) {

        if (!ADMIN_ROLE.equalsIgnoreCase(role)) {
            throw new SecurityException("Akses Ditolak: Hanya Admin yang dapat menambah kategori.");
        }

        Category savedCategory = categoryService.createCategory(category);
        return new ResponseEntity<>(savedCategory, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @PathVariable Integer id,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role,
            @RequestBody Category categoryDetails) {

        if (!ADMIN_ROLE.equalsIgnoreCase(role)) {
            throw new SecurityException("Akses Ditolak: Hanya Admin yang dapat mengubah kategori.");
        }

        Category updatedCategory = categoryService.updateCategory(id, categoryDetails);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Integer id,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {

        if (!ADMIN_ROLE.equalsIgnoreCase(role)) {
            throw new SecurityException("Akses Ditolak: Hanya Admin yang dapat menghapus kategori.");
        }

        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}

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

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequirePermission;
import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.CategoryCreateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.CategoryResponse;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;

@RestController("catalogCategoryController")
@RequestMapping("/api/catalog/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/main")
    public ResponseEntity<List<CategoryResponse>> getMainCategories() {
        return ResponseEntity.ok(categoryService.getMainCategories());
    }

    @GetMapping("/sub/{parentId}")
    public ResponseEntity<List<CategoryResponse>> getSubCategories(@PathVariable Integer parentId) {
        return ResponseEntity.ok(categoryService.getSubCategories(parentId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Integer id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    @PostMapping
    @RequirePermission(PermissionValue.CATALOG_CREATE_CATEGORY)
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse savedCategory = categoryService.createCategory(request);
        return new ResponseEntity<>(savedCategory, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @RequirePermission(PermissionValue.CATALOG_UPDATE_CATEGORY)
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Integer id,
            @Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse updatedCategory = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(PermissionValue.CATALOG_DELETE_CATEGORY)
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}

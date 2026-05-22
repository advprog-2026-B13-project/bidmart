package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.CategoryCreateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.CategoryResponse;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setImageUrl(request.getImageUrl());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Parent kategori dengan ID " + request.getParentId() + " tidak ditemukan"));
            category.setParentCategory(parent);
        }

        Category saved = categoryRepository.save(category);
        log.info("Category created: categoryId={}, name={}", saved.getId(), saved.getName());
        return CategoryResponse.from(saved);
    }

    @Override
    public List<CategoryResponse> getMainCategories() {
        return categoryRepository.findByParentCategoryIsNull().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Override
    public List<CategoryResponse> getSubCategories(Integer parentId) {
        return categoryRepository.findByParentCategoryId(parentId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Override
    public CategoryResponse getCategoryById(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kategori dengan ID " + id + " tidak ditemukan"));
        return CategoryResponse.from(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Integer id, CategoryCreateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kategori dengan ID " + id + " tidak ditemukan"));
        category.setName(request.getName());
        category.setImageUrl(request.getImageUrl());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Parent kategori dengan ID " + request.getParentId() + " tidak ditemukan"));
            category.setParentCategory(parent);
        } else {
            category.setParentCategory(null);
        }

        Category updated = categoryRepository.save(category);
        log.info("Category updated: categoryId={}, name={}", updated.getId(), updated.getName());
        return CategoryResponse.from(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Integer id) {
        log.info("Category delete: categoryId={}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kategori dengan ID " + id + " tidak ditemukan"));
        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            log.warn("Category delete rejected - has subcategories: categoryId={}", id);
            throw new IllegalStateException("Kategori tidak bisa dihapus karena masih memiliki sub-kategori.");
        }
        categoryRepository.delete(category);
        log.info("Category deleted: categoryId={}", id);
    }
}

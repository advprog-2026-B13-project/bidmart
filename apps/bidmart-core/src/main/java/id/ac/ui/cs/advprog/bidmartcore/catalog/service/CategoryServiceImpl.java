package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    public List<Category> getMainCategories() {
        return categoryRepository.findByParentCategoryIsNull();
    }

    @Override
    public List<Category> getSubCategories(Integer parentId) {
        return categoryRepository.findByParentCategoryId(parentId);
    }

    @Override
    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kategori dengan ID " + id + " tidak ditemukan"));
    }

    @Override
    @Transactional
    public Category updateCategory(Integer id, Category categoryDetails) {
        Category category = getCategoryById(id);
        category.setName(categoryDetails.getName());
        category.setParentCategory(categoryDetails.getParentCategory());
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Integer id) {
        Category category = getCategoryById(id);
        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            throw new IllegalStateException("Kategori tidak bisa dihapus karena masih memiliki sub-kategori.");
        }
        categoryRepository.delete(category);
    }
}
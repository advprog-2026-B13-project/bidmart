package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.CategoryCreateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.CategoryResponse;
import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryCreateRequest request);
    List<CategoryResponse> getMainCategories();
    List<CategoryResponse> getSubCategories(Integer parentId);
    CategoryResponse getCategoryById(Integer id);
    CategoryResponse updateCategory(Integer id, CategoryCreateRequest request);
    void deleteCategory(Integer id);
}

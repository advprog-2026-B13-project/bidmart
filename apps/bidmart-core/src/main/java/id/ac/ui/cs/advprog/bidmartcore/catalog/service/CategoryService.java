package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;
import java.util.List;

public interface CategoryService {
    Category createCategory(Category category);
    List<Category> getMainCategories();
    List<Category> getSubCategories(Integer parentId);
    Category getCategoryById(Integer id);
    Category updateCategory(Integer id, Category categoryDetails);
    void deleteCategory(Integer id);
}
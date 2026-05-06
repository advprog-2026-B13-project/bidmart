package id.ac.ui.cs.advprog.bidmartcore.catalog.dto;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategoryResponse {

    private final Integer id;
    private final String name;
    private final Integer parentId;

    public static CategoryResponse from(Category category) {
        Integer parentId = category.getParentCategory() != null
                ? category.getParentCategory().getId()
                : null;
        return new CategoryResponse(category.getId(), category.getName(), parentId);
    }
}

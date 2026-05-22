package id.ac.ui.cs.advprog.bidmartcore.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryCreateRequest {

    @NotBlank(message = "Nama kategori tidak boleh kosong")
    private String name;

    private String imageUrl;

    @Positive(message = "Parent ID harus lebih dari nol")
    private Integer parentId;
}

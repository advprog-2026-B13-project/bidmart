package id.ac.ui.cs.advprog.bidmartcore.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListingUpdateRequest {
    @NotBlank(message = "Deskripsi tidak boleh kosong")
    private String description;
    private String imageUrl;
}

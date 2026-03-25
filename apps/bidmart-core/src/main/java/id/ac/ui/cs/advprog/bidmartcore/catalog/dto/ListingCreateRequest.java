package id.ac.ui.cs.advprog.bidmartcore.catalog.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class ListingCreateRequest {
    @NotNull(message = "Seller ID wajib diisi")
    private UUID sellerId;

    @NotNull(message = "Kategori ID wajib diisi")
    private Integer categoryId;

    @NotBlank(message = "Judul tidak boleh kosong")
    private String title;

    @NotBlank(message = "Deskripsi tidak boleh kosong")
    private String description;

    private String imageUrl;

    @NotNull(message = "Harga awal wajib diisi")
    @PositiveOrZero(message = "Harga awal tidak boleh negatif")
    private BigDecimal startingPrice;

    @NotNull(message = "Harga cadangan wajib diisi")
    @PositiveOrZero(message = "Harga cadangan tidak boleh negatif")
    private BigDecimal reservePrice;

    @NotNull(message = "Minimal increment wajib diisi")
    @Positive(message = "Minimal increment harus lebih dari nol")
    private BigDecimal minBidIncrement;

    @NotNull(message = "Waktu mulai wajib diisi")
    private LocalDateTime startTime;

    @NotNull(message = "Waktu berakhir wajib diisi")
    @Future(message = "Waktu berakhir harus di masa depan")
    private LocalDateTime endTime;
}

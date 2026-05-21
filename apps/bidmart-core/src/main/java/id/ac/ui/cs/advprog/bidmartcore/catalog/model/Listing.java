package id.ac.ui.cs.advprog.bidmartcore.catalog.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "listings")
@Getter
@Setter
@NoArgsConstructor
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Seller ID tidak boleh kosong")
    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotBlank(message = "Judul tidak boleh kosong")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Deskripsi tidak boleh kosong")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @NotNull(message = "Harga awal harus diisi")
    @PositiveOrZero(message = "Harga awal tidak boleh negatif")
    @Column(name = "starting_price", nullable = false)
    private BigDecimal startingPrice;

    @NotNull(message = "Harga cadangan harus diisi")
    @PositiveOrZero(message = "Harga cadangan tidak boleh negatif")
    @Column(name = "reserve_price", nullable = false)
    private BigDecimal reservePrice;

    @NotNull(message = "Minimal increment harus diisi")
    @Positive(message = "Minimal increment harus lebih dari nol")
    @Column(name = "min_bid_increment", nullable = false)
    private BigDecimal minBidIncrement;

    @Column(name = "current_price", nullable = false)
    private BigDecimal currentPrice;

    @NotNull(message = "Jumlah bid tidak boleh kosong")
    @Column(name = "bid_count", nullable = false)
    private Integer bidCount = 0;

    @NotNull(message = "Waktu mulai tidak boleh kosong")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "Waktu selesai tidak boleh kosong")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status = ListingStatus.DRAFT;

    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(name = "moderated_by_admin_id")
    private UUID moderatedByAdminId;

    @Column(name = "takedown_reason", columnDefinition = "TEXT")
    private String takedownReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersistSetup() {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Waktu selesai lelang harus setelah waktu mulai");
        }

        if (currentPrice == null) {
            currentPrice = startingPrice;
        }

        if (minBidIncrement == null) {
            minBidIncrement = BigDecimal.ONE;
        }

        if (bidCount == null) {
            bidCount = 0;
        }

        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdateSetup() {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Waktu selesai lelang harus setelah waktu mulai");
        }

        updatedAt = LocalDateTime.now();
    }
}
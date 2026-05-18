package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// JPA annotations here for Spring Data JPA adapter
@Entity
@Table(name = "bids", indexes = {
    @Index(name = "idx_bids_listing_id", columnList = "listing_id"),
    @Index(name = "idx_bids_bidder_id", columnList = "bidder_id"),
    @Index(name = "idx_bids_listing_bidder", columnList = "listing_id, bidder_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "bidder_id", nullable = false)
    private UUID bidderId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "max_amount", precision = 19, scale = 2)
    private BigDecimal maxAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private BidSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BidStatus.ACCEPTED;
        }
        if (source == null) {
            source = BidSource.MANUAL;
        }
        if (maxAmount == null) {
            maxAmount = amount;
        }
    }
}

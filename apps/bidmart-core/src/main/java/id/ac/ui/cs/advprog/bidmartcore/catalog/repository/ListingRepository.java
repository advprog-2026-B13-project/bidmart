package id.ac.ui.cs.advprog.bidmartcore.catalog.repository;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository("catalogListingRepository")
public interface ListingRepository extends JpaRepository<Listing, UUID>, JpaSpecificationExecutor<Listing> {
    List<Listing> findByTitleContainingIgnoreCase(String keyword);
    List<Listing> findByCategoryId(Integer categoryId);
    List<Listing> findBySellerId(UUID sellerId);
    @Modifying
    @Query("UPDATE Listing l SET l.bidCount = COALESCE(l.bidCount, 0) + 1, l.currentPrice = :price, l.winnerId = :winnerId WHERE l.id = :id")
    void recordNewBid(@Param("id") UUID id, @Param("price") BigDecimal price, @Param("winnerId") UUID winnerId);
    List<Listing> findByStatus(ListingStatus status);
}
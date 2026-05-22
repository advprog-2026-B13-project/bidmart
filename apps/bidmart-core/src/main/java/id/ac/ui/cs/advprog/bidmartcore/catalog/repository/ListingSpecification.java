package id.ac.ui.cs.advprog.bidmartcore.catalog.repository;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ListingSpecification {

    private static final String CURRENT_PRICE = "currentPrice";

    public static Specification<Listing> hasTitle(String keyword) {
        return (root, query, cb) -> keyword == null ? null :
                cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Listing> hasPriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (minPrice == null && maxPrice == null) return null;
            if (minPrice != null && maxPrice != null) return cb.between(root.get(CURRENT_PRICE), minPrice, maxPrice);
            if (minPrice != null) return cb.greaterThanOrEqualTo(root.get(CURRENT_PRICE), minPrice);
            return cb.lessThanOrEqualTo(root.get(CURRENT_PRICE), maxPrice);
        };
    }

    public static Specification<Listing> isNotExpired() {
        return (root, query, cb) -> cb.greaterThan(root.get("endTime"), LocalDateTime.now());
    }

    public static Specification<Listing> hasCategoryIn(List<Integer> categoryIds) {
        return (root, query, cb) -> (categoryIds == null || categoryIds.isEmpty()) ? null :
                root.get("category").get("id").in(categoryIds);
    }

    public static Specification<Listing> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), ListingStatus.ACTIVE);
    }

    public static Specification<Listing> hasStatus(ListingStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Listing> hasTitleOrDescription(String keyword) {
        return (root, query, cb) -> keyword == null ? null :
                cb.or(
                        cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%")
                );
    }
}

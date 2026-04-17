package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.CategoryRepository;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.ListingRepository;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.ListingSpecification;
import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingCreateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingUpdateRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;

    public ListingServiceImpl(ListingRepository listingRepository, CategoryRepository categoryRepository) {
        this.listingRepository = listingRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public Listing createListing(ListingCreateRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Kategori tidak ditemukan"));
        Listing listing = new Listing();
        listing.setSellerId(request.getSellerId());
        listing.setCategory(category);
        listing.setTitle(request.getTitle());
        listing.setDescription(request.getDescription());
        listing.setImageUrl(request.getImageUrl());
        listing.setStartingPrice(request.getStartingPrice());
        listing.setReservePrice(request.getReservePrice());
        listing.setMinBidIncrement(request.getMinBidIncrement());
        listing.setStartTime(request.getStartTime());
        listing.setEndTime(request.getEndTime());
        listing.setStatus(ListingStatus.DRAFT);

        return listingRepository.save(listing);
    }

    @Override
    @Transactional
    public Listing updateListing(UUID id, UUID requesterId, ListingUpdateRequest request) {
        Listing existingListing = listingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing dengan ID tersebut tidak ditemukan"));
        if (!existingListing.getSellerId().equals(requesterId)) {
            throw new SecurityException("Akses ditolak: Anda bukan pemilik listing ini.");
        }
        if (existingListing.getStatus() != ListingStatus.DRAFT) {
            throw new IllegalStateException("Hanya listing berstatus DRAFT yang dapat diubah.");
        }
        if (existingListing.getBidCount() != null && existingListing.getBidCount() > 0) {
            throw new IllegalStateException("Gagal memperbarui: Listing ini sudah memiliki penawaran.");
        }

        existingListing.setDescription(request.getDescription());
        existingListing.setImageUrl(request.getImageUrl());
        return listingRepository.save(existingListing);
    }

    @Override
    public Listing getListingById(UUID id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing dengan ID tersebut tidak ditemukan"));
    }

    @Override
    @Transactional
    public void deleteListing(UUID id, UUID requesterId) {
        Listing existingListing = listingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing dengan ID tersebut tidak ditemukan"));
        if (!existingListing.getSellerId().equals(requesterId)) {
            throw new SecurityException("Akses ditolak: Anda bukan pemilik listing ini.");
        }
        if (existingListing.getStatus() != ListingStatus.DRAFT) {
            throw new IllegalStateException("Hanya listing berstatus DRAFT yang dapat dibatalkan.");
        }
        if (existingListing.getBidCount() != null && existingListing.getBidCount() > 0) {
            throw new IllegalStateException("Gagal membatalkan: Listing ini sudah memiliki penawaran.");
        }

        listingRepository.deleteById(id);
    }

    private List<Integer> getAllCategoryIdsWithChildren(Integer parentId) {
        List<Integer> ids = new ArrayList<>();
        ids.add(parentId);

        List<Category> children = categoryRepository.findByParentCategoryId(parentId);
        for (Category child : children) {
            ids.addAll(getAllCategoryIdsWithChildren(child.getId()));
        }
        return ids;
    }

    @Override
    @Transactional
    public void updateCurrentPriceAndWinner(UUID listingId, BigDecimal newPrice, UUID winnerId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing tidak ditemukan"));
        listing.setCurrentPrice(newPrice);
        listing.setWinnerId(winnerId);
        Integer currentBids = listing.getBidCount();
        listing.setBidCount((currentBids == null ? 0 : currentBids) + 1);
        listingRepository.save(listing);
    }

    @Override
    @Transactional
    public void updateStatus(UUID listingId, ListingStatus status) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing tidak ditemukan"));
        listing.setStatus(status);
        listingRepository.save(listing);
    }

    @Override
    @Transactional
    public void updateEndTime(UUID listingId, LocalDateTime endTime) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing tidak ditemukan"));
        listing.setEndTime(endTime);
        listingRepository.save(listing);
    }

    @Override
    public boolean isListingValidForBid(UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElse(null);
        if (listing == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return listing.getStatus() == ListingStatus.ACTIVE ||
                listing.getStatus() == ListingStatus.EXTENDED &&
                !now.isBefore(listing.getStartTime()) &&
                now.isBefore(listing.getEndTime());
    }

    @Override
    public Page<Listing> searchListings(String keyword, BigDecimal minPrice, BigDecimal maxPrice, Integer categoryId, Pageable pageable) {
        Specification<Listing> spec = Specification.where(ListingSpecification.isActive())
                .and(ListingSpecification.isNotExpired());

        if (keyword != null && !keyword.trim().isEmpty()) {
            spec = spec.and(ListingSpecification.hasTitle(keyword));
        }

        if (minPrice != null || maxPrice != null) {
            spec = spec.and(ListingSpecification.hasPriceBetween(minPrice, maxPrice));
        }

        if (categoryId != null) {
            List<Integer> categoryIds = getAllCategoryIdsWithChildren(categoryId);
            spec = spec.and(ListingSpecification.hasCategoryIn(categoryIds));
        }

        return listingRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional
    public void updateFinalResult(UUID listingId, BigDecimal finalPrice, UUID winnerId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing tidak ditemukan"));

        listing.setCurrentPrice(finalPrice);
        listing.setWinnerId(winnerId);
        listingRepository.save(listing);
    }
}
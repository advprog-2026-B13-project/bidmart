package id.ac.ui.cs.advprog.bidmartcore.catalog.controller;

import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingCreateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingUpdateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @GetMapping("/search")
    public ResponseEntity<Page<Listing>> searchListings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "endTime"));
        Page<Listing> results = listingService.searchListings(keyword, minPrice, maxPrice, categoryId, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<Listing> getListingById(@PathVariable UUID id) {
        Listing listing = listingService.getListingById(id);
        return ResponseEntity.ok(listing);
    }

    @PostMapping("/create")
    public ResponseEntity<Listing> createListing(@Valid @RequestBody ListingCreateRequest requestDTO) {
        Listing savedListing = listingService.createListing(requestDTO);
        return new ResponseEntity<>(savedListing, HttpStatus.CREATED);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Listing> updateListing(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID requesterId,
            @Valid @RequestBody ListingUpdateRequest requestDTO) {

        Listing updatedListing = listingService.updateListing(id, requesterId, requestDTO);
        return ResponseEntity.ok(updatedListing);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteListing(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID requesterId) {
        listingService.deleteListing(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/validate")
    public ResponseEntity<Boolean> validateListingForBid(@PathVariable UUID id) {
        boolean isValid = listingService.isListingValidForBid(id);
        return ResponseEntity.ok(isValid);
    }
}
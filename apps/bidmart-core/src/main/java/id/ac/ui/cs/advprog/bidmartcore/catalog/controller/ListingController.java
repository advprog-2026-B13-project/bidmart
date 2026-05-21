package id.ac.ui.cs.advprog.bidmartcore.catalog.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequireLogin;
import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingCreateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingUpdateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController("catalogListingController")
@RequestMapping("/api/catalog/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;
    private final AuthContext authContext;

    @GetMapping("/search")
    public ResponseEntity<Page<Listing>> searchListings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) ListingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "endTime"));
        if (status == ListingStatus.DRAFT) {
            return ResponseEntity.ok(Page.empty(pageable));
        }
        Page<Listing> results = listingService.searchListings(keyword, minPrice, maxPrice, categoryId, status, pageable);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<Listing> getListingById(@PathVariable UUID id) {
        Listing listing = listingService.getListingById(id);
        if (listing.getStatus() == ListingStatus.DRAFT) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(listing);
    }

    @GetMapping("/detail/{id}/owner")
    @RequireLogin
    public ResponseEntity<Listing> getListingByOwner(@PathVariable UUID id) {
        Listing listing = listingService.getListingForOwner(id, authContext.getUserId());
        return ResponseEntity.ok(listing);
    }

    @GetMapping("/mine")
    @RequireLogin
    public ResponseEntity<List<Listing>> getMyListings() {
        List<Listing> listings = listingService.getListingsBySeller(authContext.getUserId());
        return ResponseEntity.ok(listings);
    }

    @PostMapping("/create")
    @RequireLogin
    public ResponseEntity<Listing> createListing(
            @Valid @RequestBody ListingCreateRequest requestDTO) {
        Listing savedListing = listingService.createListing(requestDTO, authContext.getUserId());
        return new ResponseEntity<>(savedListing, HttpStatus.CREATED);
    }

    @PutMapping("/update/{id}")
    @RequireLogin
    public ResponseEntity<Listing> updateListing(
            @PathVariable UUID id,
            @Valid @RequestBody ListingUpdateRequest requestDTO) {
        Listing updatedListing = listingService.updateListing(id, authContext.getUserId(), requestDTO);
        return ResponseEntity.ok(updatedListing);
    }

    @PutMapping("/{id}/activate")
    @RequireLogin
    public ResponseEntity<Listing> activateListing(@PathVariable UUID id) {
        Listing listing = listingService.activateListing(id, authContext.getUserId());
        return ResponseEntity.ok(listing);
    }

    @PutMapping("/{id}/close")
    @RequireLogin
    public ResponseEntity<Listing> closeListing(@PathVariable UUID id) {
        Listing listing = listingService.closeListing(id, authContext.getUserId());
        return ResponseEntity.ok(listing);
    }

    @DeleteMapping("/delete/{id}")
    @RequireLogin
    public ResponseEntity<Void> deleteListing(
            @PathVariable UUID id) {
        listingService.deleteListing(id, authContext.getUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/validate")
    public ResponseEntity<Boolean> validateListingForBid(@PathVariable UUID id) {
        boolean isValid = listingService.isListingValidForBid(id);
        return ResponseEntity.ok(isValid);
    }
}
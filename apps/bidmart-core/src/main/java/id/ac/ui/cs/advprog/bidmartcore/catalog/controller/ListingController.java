package id.ac.ui.cs.advprog.bidmartcore.catalog.controller;

import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingCreateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.dto.ListingUpdateRequest;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Category;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmartcore.catalog.service.ListingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping("/create")
    public ResponseEntity<Listing> createListing(@Valid @RequestBody ListingCreateRequest requestDTO) {
        Listing newListing = new Listing();
        newListing.setSellerId(requestDTO.getSellerId());
        newListing.setTitle(requestDTO.getTitle());
        newListing.setDescription(requestDTO.getDescription());
        newListing.setImageUrl(requestDTO.getImageUrl());
        newListing.setStartingPrice(requestDTO.getStartingPrice());
        newListing.setReservePrice(requestDTO.getReservePrice());
        newListing.setMinBidIncrement(requestDTO.getMinBidIncrement());
        newListing.setStartTime(requestDTO.getStartTime());
        newListing.setEndTime(requestDTO.getEndTime());
        Category category = new Category();
        category.setId(requestDTO.getCategoryId());
        newListing.setCategory(category);

        Listing savedListing = listingService.createListing(newListing);
        return new ResponseEntity<>(savedListing, HttpStatus.CREATED);
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<Listing> getListingById(@PathVariable UUID id) {
        Listing listing = listingService.getListingById(id);
        return ResponseEntity.ok(listing);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Listing> updateListing(
            @PathVariable UUID id,
            @Valid @RequestBody ListingUpdateRequest requestDTO) {

        Listing existing = listingService.getListingById(id);
        if (existing.getStatus() != ListingStatus.DRAFT) {
            return ResponseEntity.badRequest().build();
        }

        Listing updateData = new Listing();
        updateData.setDescription(requestDTO.getDescription());
        updateData.setImageUrl(requestDTO.getImageUrl());

        Listing updatedListing = listingService.updateListing(id, updateData);
        return ResponseEntity.ok(updatedListing);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteListing(@PathVariable UUID id) {
        listingService.deleteListing(id);
        return ResponseEntity.noContent().build();
    }
}
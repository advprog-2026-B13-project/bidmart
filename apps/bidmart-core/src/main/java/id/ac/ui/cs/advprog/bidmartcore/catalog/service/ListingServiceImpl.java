package id.ac.ui.cs.advprog.bidmartcore.catalog.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.ac.ui.cs.advprog.bidmartcore.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmartcore.catalog.repository.ListingRepository;

@Service
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;

    public ListingServiceImpl(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    @Override
    public Listing createListing(Listing listing) {
        listing.setId(null);
        return listingRepository.save(listing);
    }

    @Override
    public Listing updateListing(UUID id, Listing updatedData) {
        Listing existingListing = listingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing dengan ID tersebut tidak ditemukan"));
        existingListing.setDescription(updatedData.getDescription());
        existingListing.setImageUrl(updatedData.getImageUrl());
        return listingRepository.save(existingListing);
    }

    @Override
    public Listing getListingById(UUID id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Listing dengan ID tersebut tidak ditemukan"));
    }

    @Override
    public void deleteListing(UUID id) {
        if (!listingRepository.existsById(id)) {
            throw new IllegalArgumentException("Listing dengan ID tersebut tidak ditemukan");
        }
        listingRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void updateCurrentPriceAndWinner(UUID listingId, BigDecimal newPrice, UUID winnerId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing tidak ditemukan"));
        listing.setCurrentPrice(newPrice);
        listing.setWinnerId(winnerId);
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
}
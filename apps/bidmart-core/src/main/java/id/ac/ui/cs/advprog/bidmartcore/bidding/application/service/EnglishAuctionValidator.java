package id.ac.ui.cs.advprog.bidmartcore.bidding.application.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.ListingPort.ListingInfo;
import id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.config.BiddingProperties;
import id.ac.ui.cs.advprog.bidmartcore.catalog.model.ListingStatus;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnglishAuctionValidator {

    private final BiddingProperties properties;

    public void validateStatic(UUID sellerId, BigDecimal bidAmount, UUID bidderId) {
        if (sellerId.equals(bidderId)) {
            throw new IllegalArgumentException("Penjual tidak bisa menawar pada lelang sendiri");
        }

        if (bidAmount == null || bidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Harga penawaran harus lebih dari nol");
        }
    }

    public void validateDynamic(ListingInfo listing, BigDecimal bidAmount, BigDecimal currentPrice) {
        if (listing.status() != ListingStatus.ACTIVE) {
            throw new IllegalArgumentException("Lelang belum aktif atau sudah ditutup");
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (now.isAfter(listing.endTime())) {
            throw new IllegalArgumentException("Lelang sudah berakhir");
        }

        BigDecimal minRequired = (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) == 0)
                ? listing.startingPrice()
                : currentPrice.add(properties.getMinBidIncrement());

        if (bidAmount.compareTo(minRequired) < 0) {
            throw new IllegalArgumentException("Harga penawaran harus minimal " + minRequired);
        }
    }
}

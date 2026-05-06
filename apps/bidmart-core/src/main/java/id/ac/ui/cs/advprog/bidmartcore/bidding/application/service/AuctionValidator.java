package id.ac.ui.cs.advprog.bidmartcore.bidding.application.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class AuctionValidator {

    public void validateStatic(UUID sellerId, BigDecimal bidAmount, UUID bidderId) {
        if (sellerId.equals(bidderId)) {
            throw new IllegalArgumentException("Penjual tidak bisa menawar pada lelang sendiri");
        }

        if (bidAmount == null || bidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Harga penawaran harus lebih dari nol");
        }
    }
}

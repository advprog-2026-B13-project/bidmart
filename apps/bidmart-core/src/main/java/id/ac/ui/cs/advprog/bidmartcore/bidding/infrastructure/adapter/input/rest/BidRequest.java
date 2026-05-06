package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.input.rest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BidRequest {
    @NotNull(message = "Listing ID wajib diisi")
    private UUID listingId;

    @NotNull(message = "Harga penawaran wajib diisi")
    @Positive(message = "Harga penawaran harus lebih dari nol")
    private BigDecimal amount;

    @NotNull(message = "Tipe penawaran wajib diisi")
    private BidType bidType;
}

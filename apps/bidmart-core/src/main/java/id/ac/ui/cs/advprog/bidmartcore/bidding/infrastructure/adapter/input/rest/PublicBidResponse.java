package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.input.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PublicBidResponse {
    private UUID bidId;
    private UUID listingId;
    private UUID bidderId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}

package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.input.rest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuctionStatusResponse {
    private UUID listingId;
    private BigDecimal currentPrice;
    private UUID currentWinnerId;
    private BigDecimal myHighestBid;
    private LocalDateTime endTime;
    private String status;
}

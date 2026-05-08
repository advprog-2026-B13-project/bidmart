package id.ac.ui.cs.advprog.bidmartcore.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BidPlacedMessage {
    private UUID listingId;
    private BigDecimal amount;
    private UUID bidderId;
}
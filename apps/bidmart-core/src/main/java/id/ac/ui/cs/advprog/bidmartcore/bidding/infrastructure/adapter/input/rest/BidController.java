package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest.ApiResponse;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.AuthContext;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security.RequireLogin;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidType;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.input.BiddingUseCase;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.input.BiddingUseCase.AuctionStatusResult;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.input.BiddingUseCase.BidResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bidding")
@RequiredArgsConstructor
@Tag(name = "Bidding", description = "Auction bidding endpoints")
public class BidController {

    private final BiddingUseCase biddingUseCase;
    private final AuthContext authContext;

    @PostMapping("/bids")
    @RequireLogin
    @Operation(summary = "Place a bid on a listing")
    public ResponseEntity<ApiResponse<BidResponse>> placeBid(@Valid @RequestBody BidRequest request) {
        UUID bidderId = authContext.getUserId();
        BidResult result = biddingUseCase.placeBid(request.getListingId(), request.getAmount(), bidderId,
                request.getBidType());

        BidResponse response = new BidResponse(
                result.bidId(),
                result.listingId(),
                result.bidderId(),
                result.amount(),
                result.maxAmount(),
                result.source(),
                result.status(),
                result.createdAt()
        );

        return ResponseEntity.ok(ApiResponse.success("Penawaran berhasil", response));
    }

    @GetMapping("/listings/{listingId}/bids")
    @Operation(summary = "Get all bids for a listing")
    public ResponseEntity<ApiResponse<List<BidResponse>>> getBidsForListing(@PathVariable UUID listingId) {
        List<BidResult> bids = biddingUseCase.getBidsForListing(listingId);
        List<BidResponse> responses = bids.stream().map(bid -> new BidResponse(
                bid.bidId(),
                bid.listingId(),
                bid.bidderId(),
                bid.amount(),
                bid.maxAmount(),
                bid.source(),
                bid.status(),
                bid.createdAt()
        )).toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/my-bids")
    @RequireLogin
    @Operation(summary = "Get current user's bids")
    public ResponseEntity<ApiResponse<List<BidResponse>>> getMyBids() {
        UUID bidderId = authContext.getUserId();
        List<BidResult> bids = biddingUseCase.getMyBids(bidderId);
        List<BidResponse> responses = bids.stream().map(bid -> new BidResponse(
                bid.bidId(),
                bid.listingId(),
                bid.bidderId(),
                bid.amount(),
                bid.maxAmount(),
                bid.source(),
                bid.status(),
                bid.createdAt()
        )).toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/listings/{listingId}/status")
    @Operation(summary = "Get auction status for a listing")
    public ResponseEntity<ApiResponse<AuctionStatusResponse>> getAuctionStatus(@PathVariable UUID listingId) {
        AuctionStatusResult result = biddingUseCase.getAuctionStatus(listingId);
        AuctionStatusResponse response = new AuctionStatusResponse(
                result.listingId(),
                result.currentPrice(),
                result.currentWinnerId(),
                result.endTime(),
                result.status()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

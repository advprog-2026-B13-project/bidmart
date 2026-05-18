package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.input.rest;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.ProfileUseCase;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Other user's profile payload")
public class OtherUserProfileResponse {

    @Schema(description = "User unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;

    @Schema(description = "Email address", example = "user@example.com")
    private String email;

    @Schema(description = "Display name", example = "Jane Doe")
    private String displayName;

    @Schema(description = "Profile picture URL", example = "https://example.com/photo.jpg")
    private String photoUrl;

    @Schema(description = "Shipping address", example = "Jl. Margonda Raya No. 100, Depok")
    private String shippingAddress;

    @Schema(description = "Account status", example = "ACTIVE")
    private String status;

    @Schema(description = "Whether bidding history is included in this response")
    private boolean biddingHistoryVisible;

    @Schema(description = "Completed/previous bids, visible for admin only")
    private List<OtherUserBidResponse> previousBids;

    @Schema(description = "Ongoing bids (currently accepted), visible for admin only")
    private List<OtherUserBidResponse> ongoingBids;

    public static OtherUserProfileResponse fromView(ProfileUseCase.OtherUserProfileView view, boolean biddingHistoryVisible) {
        return new OtherUserProfileResponse(
                view.user().getId(),
                view.user().getEmail(),
                view.user().getDisplayName(),
                view.user().getPhotoUrl(),
                view.user().getShippingAddress(),
                view.user().getStatus() != null ? view.user().getStatus().name() : null,
                biddingHistoryVisible,
                view.previousBids().stream().map(OtherUserBidResponse::fromView).toList(),
                view.ongoingBids().stream().map(OtherUserBidResponse::fromView).toList()
        );
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Bid item in other user's profile")
    public static class OtherUserBidResponse {

        @Schema(description = "Bid identifier")
        private UUID bidId;

        @Schema(description = "Listing identifier")
        private UUID listingId;

        @Schema(description = "Bid amount")
        private java.math.BigDecimal amount;

        @Schema(description = "Bid status", example = "ACCEPTED")
        private String status;

        @Schema(description = "Bid creation timestamp", example = "2026-03-26T09:20:41.756")
        private String createdAt;

        public static OtherUserBidResponse fromView(ProfileUseCase.BidView bidView) {
            return new OtherUserBidResponse(
                    bidView.bidId(),
                    bidView.listingId(),
                    bidView.amount(),
                    bidView.status(),
                    bidView.createdAt() != null ? bidView.createdAt().toString() : null
            );
        }
    }
}


package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ProfileUseCase {
    User getProfile(UUID userId);
    User updateProfile(UUID userId, String displayName, String photoUrl, String shippingAddress);
    void deactivateAccount(UUID targetUserId);

    OtherUserProfileView getOtherUserProfile(UUID targetUserId, boolean includeBidHistory);

    record OtherUserProfileView(User user, List<BidView> previousBids, List<BidView> ongoingBids) {}

    record BidView(UUID bidId, UUID listingId, BigDecimal amount, String status, LocalDateTime createdAt) {}
}

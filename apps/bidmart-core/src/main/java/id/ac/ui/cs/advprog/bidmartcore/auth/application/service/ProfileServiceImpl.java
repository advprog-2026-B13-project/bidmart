package id.ac.ui.cs.advprog.bidmartcore.auth.application.service;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.UserStatus;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.ProfileUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionCachePort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.UserRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.Bid;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidStatus;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.BidRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileUseCase {

    private static final String USER_NOT_FOUND = "User not found";

    private final UserRepositoryPort userRepository;
    private final SessionRepositoryPort sessionRepository;
    private final SessionCachePort sessionCache;
    private final BidRepositoryPort bidRepository;

    @Override
    public User getProfile(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public User updateProfile(UUID userId, String displayName, String photoUrl, String shippingAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));

        if (displayName != null) {
            user.setDisplayName(displayName);
        }
        if (photoUrl != null) {
            user.setPhotoUrl(photoUrl);
        }
        if (shippingAddress != null) {
            user.setShippingAddress(shippingAddress);
        }

        return userRepository.save(user);
    }

    @Override
    public OtherUserProfileView getOtherUserProfile(UUID targetUserId, boolean includeBidHistory) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));

        if (!includeBidHistory) {
            return new OtherUserProfileView(targetUser, List.of(), List.of());
        }

        List<Bid> bids = bidRepository.findByBidder(targetUserId);

        List<BidView> previousBids = bids.stream()
                .filter(bid -> bid.getStatus() != BidStatus.ACCEPTED)
                .map(bid -> new BidView(
                        bid.getId(),
                        bid.getListingId(),
                        bid.getAmount(),
                        bid.getStatus() != null ? bid.getStatus().name() : null,
                        bid.getCreatedAt()
                ))
                .toList();

        List<BidView> ongoingBids = bids.stream()
                .filter(bid -> bid.getStatus() == BidStatus.ACCEPTED)
                .map(bid -> new BidView(
                        bid.getId(),
                        bid.getListingId(),
                        bid.getAmount(),
                        bid.getStatus().name(),
                        bid.getCreatedAt()
                ))
                .toList();

        return new OtherUserProfileView(targetUser, previousBids, ongoingBids);
    }

    @Override
    @Transactional
    public void deactivateAccount(UUID targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND));

        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        // Revoke all sessions for the deactivated user
        List<Session> sessions = sessionRepository.findAllByUserId(targetUserId);
        for (Session session : sessions) {
            if (session.isActive()) {
                session.setActive(false);
                sessionRepository.save(session);
                sessionCache.evictSession(session.getId());
            }
        }
    }
}

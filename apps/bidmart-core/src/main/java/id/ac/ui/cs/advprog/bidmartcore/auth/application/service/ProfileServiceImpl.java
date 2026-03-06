package id.ac.ui.cs.advprog.bidmartcore.auth.application.service;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.UserStatus;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input.ProfileUseCase;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionCachePort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileUseCase {

    private final UserRepositoryPort userRepository;
    private final SessionRepositoryPort sessionRepository;
    private final SessionCachePort sessionCache;

    @Override
    public User getProfile(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Override
    @Transactional
    public User updateProfile(UUID userId, String displayName, String photoUrl, String shippingAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

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
    @Transactional
    public void deactivateAccount(UUID targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

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


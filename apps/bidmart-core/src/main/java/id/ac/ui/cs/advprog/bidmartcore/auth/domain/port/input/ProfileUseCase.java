package id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.input;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;

import java.util.UUID;

public interface ProfileUseCase {
    User getProfile(UUID userId);
    User updateProfile(UUID userId, String displayName, String photoUrl, String shippingAddress);
    void deactivateAccount(UUID targetUserId);
}


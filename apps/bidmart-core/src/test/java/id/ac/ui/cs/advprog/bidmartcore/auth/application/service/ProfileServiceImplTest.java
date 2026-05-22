package id.ac.ui.cs.advprog.bidmartcore.auth.application.service;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.MFAType;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.UserStatus;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionCachePort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.UserRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.Bid;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidStatus;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.BidRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private SessionRepositoryPort sessionRepository;

    @Mock
    private SessionCachePort sessionCache;

    @Mock
    private BidRepositoryPort bidRepository;

    private ProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProfileServiceImpl(userRepository, sessionRepository, sessionCache, bidRepository);
    }

    @Test
    void getProfile_success() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = service.getProfile(userId);

        assertEquals(userId, result.getId());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getProfile_notFound_shouldThrow() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getProfile(userId));
    }

    @Test
    void updateProfile_allFields_shouldUpdate() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setDisplayName("Old");
        user.setPhotoUrl("old.jpg");
        user.setShippingAddress("Old Address");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = service.updateProfile(userId, "New", "new.jpg", "New Address");

        assertEquals("New", result.getDisplayName());
        assertEquals("new.jpg", result.getPhotoUrl());
        assertEquals("New Address", result.getShippingAddress());
    }

    @Test
    void updateProfile_nullFields_shouldKeepOld() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setDisplayName("Old");
        user.setPhotoUrl("old.jpg");
        user.setShippingAddress("Old Address");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        User result = service.updateProfile(userId, null, null, null);

        assertEquals("Old", result.getDisplayName());
        assertEquals("old.jpg", result.getPhotoUrl());
        assertEquals("Old Address", result.getShippingAddress());
    }

    @Test
    void updateProfile_notFound_shouldThrow() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.updateProfile(userId, "name", null, null));
    }

    @Test
    void deactivateAccount_success_shouldSuspendAndRevokeSessions() {
        UUID targetId = UUID.randomUUID();
        User user = new User();
        user.setId(targetId);
        user.setStatus(UserStatus.ACTIVE);

        Session active = new Session();
        active.setId("active-session");
        active.setActive(true);

        Session inactive = new Session();
        inactive.setId("inactive-session");
        inactive.setActive(false);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(user));
        when(sessionRepository.findAllByUserId(targetId)).thenReturn(List.of(active, inactive));
        when(userRepository.save(any(User.class))).thenReturn(user);

        service.deactivateAccount(targetId);

        assertEquals(UserStatus.SUSPENDED, user.getStatus());
        verify(userRepository).save(user);
        assertFalse(active.isActive());
        verify(sessionRepository).save(active);
        verify(sessionCache).evictSession("active-session");
        verify(sessionCache, never()).evictSession("inactive-session");
    }

    @Test
    void deactivateAccount_notFound_shouldThrow() {
        UUID targetId = UUID.randomUUID();
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deactivateAccount(targetId));
    }

    @Test
    void getOtherUserProfile_whenNotAdmin_shouldHideBiddingHistory() {
        UUID userId = UUID.randomUUID();
        User target = new User();
        target.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(target));

        var result = service.getOtherUserProfile(userId, false);

        assertEquals(userId, result.user().getId());
        assertEquals(0, result.previousBids().size());
        assertEquals(0, result.ongoingBids().size());
        verifyNoInteractions(bidRepository);
    }

    @Test
    void getOtherUserProfile_whenAdmin_shouldSplitPreviousAndOngoingBids() {
        UUID userId = UUID.randomUUID();
        User target = new User();
        target.setId(userId);

        Bid accepted = new Bid();
        accepted.setId(UUID.randomUUID());
        accepted.setListingId(UUID.randomUUID());
        accepted.setBidderId(userId);
        accepted.setAmount(BigDecimal.valueOf(150000));
        accepted.setStatus(BidStatus.ACCEPTED);
        accepted.setCreatedAt(LocalDateTime.now());

        Bid outbid = new Bid();
        outbid.setId(UUID.randomUUID());
        outbid.setListingId(UUID.randomUUID());
        outbid.setBidderId(userId);
        outbid.setAmount(BigDecimal.valueOf(120000));
        outbid.setStatus(BidStatus.OUTBID);
        outbid.setCreatedAt(LocalDateTime.now().minusMinutes(3));

        Bid won = new Bid();
        won.setId(UUID.randomUUID());
        won.setListingId(UUID.randomUUID());
        won.setBidderId(userId);
        won.setAmount(BigDecimal.valueOf(200000));
        won.setStatus(BidStatus.WON);
        won.setCreatedAt(LocalDateTime.now().minusMinutes(10));

        when(userRepository.findById(userId)).thenReturn(Optional.of(target));
        when(bidRepository.findByBidder(userId)).thenReturn(List.of(accepted, outbid, won));

        var result = service.getOtherUserProfile(userId, true);

        assertEquals(2, result.previousBids().size());
        assertEquals(1, result.ongoingBids().size());
        assertEquals(BidStatus.ACCEPTED.name(), result.ongoingBids().get(0).status());
    }

    @Test
    void getOtherUserProfile_whenUserNotFound_shouldThrow() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getOtherUserProfile(userId, true));
    }

    @Test
    void getOtherUserProfile_withNullBidStatus_shouldHandleGracefully() {
        UUID userId = UUID.randomUUID();
        User target = new User();
        target.setId(userId);

        Bid nullStatus = new Bid();
        nullStatus.setId(UUID.randomUUID());
        nullStatus.setListingId(UUID.randomUUID());
        nullStatus.setBidderId(userId);
        nullStatus.setAmount(BigDecimal.valueOf(100));
        nullStatus.setStatus(null);
        nullStatus.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(target));
        when(bidRepository.findByBidder(userId)).thenReturn(List.of(nullStatus));

        var result = service.getOtherUserProfile(userId, true);

        assertEquals(1, result.previousBids().size());
        assertNull(result.previousBids().get(0).status());
    }
}

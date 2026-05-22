package id.ac.ui.cs.advprog.bidmartcore.auth.application.service;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionCachePort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.UserRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.Bid;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model.BidStatus;
import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.BidRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
}

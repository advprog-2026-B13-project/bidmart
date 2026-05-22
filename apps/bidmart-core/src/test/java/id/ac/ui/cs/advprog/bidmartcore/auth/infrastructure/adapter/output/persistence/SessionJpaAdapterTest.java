package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.SessionSpringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionJpaAdapterTest {

    @Mock
    private SessionSpringRepository springRepository;

    private SessionJpaAdapter adapter;

    @BeforeEach
    void unit_setUp() {
        adapter = new SessionJpaAdapter(springRepository);
    }

    @Test
    void unit_save_delegatesToSpringRepository() {
        Session session = new Session();
        when(springRepository.save(session)).thenReturn(session);

        Session result = adapter.save(session);

        assertSame(session, result);
    }

    @Test
    void unit_findById_found() {
        Session session = new Session();
        when(springRepository.findById("sess-1")).thenReturn(Optional.of(session));

        Optional<Session> result = adapter.findById("sess-1");

        assertTrue(result.isPresent());
        assertSame(session, result.get());
    }

    @Test
    void unit_findById_notFound() {
        when(springRepository.findById("sess-1")).thenReturn(Optional.empty());

        Optional<Session> result = adapter.findById("sess-1");

        assertTrue(result.isEmpty());
    }

    @Test
    void unit_findAllByUserId() {
        Session s1 = new Session();
        Session s2 = new Session();
        UUID userId = UUID.randomUUID();
        when(springRepository.findAllByUserId(userId)).thenReturn(List.of(s1, s2));

        List<Session> result = adapter.findAllByUserId(userId);

        assertEquals(2, result.size());
    }

    @Test
    void unit_deleteById_softDeletesSession() {
        Session session = new Session();
        session.setActive(true);
        when(springRepository.findById("sess-1")).thenReturn(Optional.of(session));
        when(springRepository.save(session)).thenReturn(session);

        adapter.deleteById("sess-1");

        assertFalse(session.isActive());
        verify(springRepository).save(session);
    }

    @Test
    void unit_deleteById_sessionNotFound_doesNothing() {
        when(springRepository.findById("nonexistent")).thenReturn(Optional.empty());

        adapter.deleteById("nonexistent");

        verify(springRepository, never()).save(any());
    }

    @Test
    void unit_deleteAllByUserIdExcept_delegates() {
        UUID userId = UUID.randomUUID();

        adapter.deleteAllByUserIdExcept(userId, "current-sess");

        verify(springRepository).deactivateAllByUserIdExcept(userId, "current-sess");
    }

    @Test
    void unit_countActiveByUserId() {
        UUID userId = UUID.randomUUID();
        when(springRepository.countActiveByUserId(userId)).thenReturn(3);

        assertEquals(3, adapter.countActiveByUserId(userId));
    }
}

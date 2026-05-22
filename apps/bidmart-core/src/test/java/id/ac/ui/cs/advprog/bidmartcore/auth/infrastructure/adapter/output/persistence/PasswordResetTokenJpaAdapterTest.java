package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.PasswordResetToken;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.PasswordResetTokenSpringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenJpaAdapterTest {

    @Mock
    private PasswordResetTokenSpringRepository springRepository;

    private PasswordResetTokenJpaAdapter adapter;

    @BeforeEach
    void unit_setUp() {
        adapter = new PasswordResetTokenJpaAdapter(springRepository);
    }

    @Test
    void unit_save() {
        PasswordResetToken token = new PasswordResetToken();
        when(springRepository.save(token)).thenReturn(token);

        PasswordResetToken result = adapter.save(token);

        assertSame(token, result);
    }

    @Test
    void unit_findById_found() {
        PasswordResetToken token = new PasswordResetToken();
        UUID id = UUID.randomUUID();
        when(springRepository.findById(id)).thenReturn(Optional.of(token));

        Optional<PasswordResetToken> result = adapter.findById(id);

        assertTrue(result.isPresent());
    }

    @Test
    void unit_findById_notFound() {
        UUID id = UUID.randomUUID();
        when(springRepository.findById(id)).thenReturn(Optional.empty());

        Optional<PasswordResetToken> result = adapter.findById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    void unit_invalidateAllByUserId() {
        UUID userId = UUID.randomUUID();

        adapter.invalidateAllByUserId(userId);

        verify(springRepository).invalidateAllByUserId(userId);
    }
}

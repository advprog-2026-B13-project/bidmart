package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.TotpCredential;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.TotpCredentialSpringRepository;
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
class TotpCredentialJpaAdapterTest {

    @Mock
    private TotpCredentialSpringRepository springRepository;

    private TotpCredentialJpaAdapter adapter;

    @BeforeEach
    void unit_setUp() {
        adapter = new TotpCredentialJpaAdapter(springRepository);
    }

    @Test
    void unit_save() {
        TotpCredential cred = new TotpCredential();
        when(springRepository.save(cred)).thenReturn(cred);

        TotpCredential result = adapter.save(cred);

        assertSame(cred, result);
    }

    @Test
    void unit_findByUserId_found() {
        TotpCredential cred = new TotpCredential();
        UUID userId = UUID.randomUUID();
        when(springRepository.findByUserId(userId)).thenReturn(Optional.of(cred));

        Optional<TotpCredential> result = adapter.findByUserId(userId);

        assertTrue(result.isPresent());
    }

    @Test
    void unit_findByUserId_notFound() {
        UUID userId = UUID.randomUUID();
        when(springRepository.findByUserId(userId)).thenReturn(Optional.empty());

        Optional<TotpCredential> result = adapter.findByUserId(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void unit_deleteByUserId() {
        UUID userId = UUID.randomUUID();

        adapter.deleteByUserId(userId);

        verify(springRepository).deleteByUserId(userId);
    }
}

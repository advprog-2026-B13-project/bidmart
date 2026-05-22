package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.EmailOtp;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.EmailOtpSpringRepository;
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
class EmailOtpJpaAdapterTest {

    @Mock
    private EmailOtpSpringRepository springRepository;

    private EmailOtpJpaAdapter adapter;

    @BeforeEach
    void unit_setUp() {
        adapter = new EmailOtpJpaAdapter(springRepository);
    }

    @Test
    void unit_save() {
        EmailOtp otp = new EmailOtp();
        when(springRepository.save(otp)).thenReturn(otp);

        EmailOtp result = adapter.save(otp);

        assertSame(otp, result);
    }

    @Test
    void unit_findLatestActiveByUserId_found() {
        EmailOtp otp = new EmailOtp();
        UUID userId = UUID.randomUUID();
        when(springRepository.findLatestActiveByUserId(userId)).thenReturn(Optional.of(otp));

        Optional<EmailOtp> result = adapter.findLatestActiveByUserId(userId);

        assertTrue(result.isPresent());
    }

    @Test
    void unit_findLatestActiveByUserId_notFound() {
        UUID userId = UUID.randomUUID();
        when(springRepository.findLatestActiveByUserId(userId)).thenReturn(Optional.empty());

        Optional<EmailOtp> result = adapter.findLatestActiveByUserId(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void unit_invalidateAllByUserId() {
        UUID userId = UUID.randomUUID();

        adapter.invalidateAllByUserId(userId);

        verify(springRepository).invalidateAllByUserId(userId);
    }
}

package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.SessionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.SessionSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SessionJpaAdapter implements SessionRepositoryPort {
    private final SessionSpringRepository springRepository;

    @Override
    public Session save(Session session) {
        return springRepository.save(session);
    }

    @Override
    public Optional<Session> findById(String sessionId) {
        return springRepository.findById(sessionId);
    }

    @Override
    public List<Session> findAllByUserId(UUID userId) {
        return springRepository.findAllByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteById(String sessionId) {
        springRepository.findById(sessionId).ifPresent(session -> {
            session.setActive(false);
            springRepository.save(session);
        });
    }

    @Override
    @Transactional
    public void deleteAllByUserIdExcept(UUID userId, String currentSessionId) {
        springRepository.deactivateAllByUserIdExcept(userId, currentSessionId);
    }

    @Override
    public int countActiveByUserId(UUID userId) {
        return springRepository.countActiveByUserId(userId);
    }
}


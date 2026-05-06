package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.UserRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.UserSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserJpaAdapter implements UserRepositoryPort {
    private final UserSpringRepository springRepository;

    @Override
    public User save(User user) {
        return springRepository.save(user);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springRepository.existsByEmail(email);
    }

    @Override
    public long countByRoleId(int roleId) {
        return springRepository.countByRoleId(roleId);
    }

    @Override
    public UserPage findUsersPage(int page, int size) {
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("email"))
        );
        Page<User> users = springRepository.findAll(pageable);
        return new UserPage(
                users.getContent(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages(),
                users.hasNext()
        );
    }
}

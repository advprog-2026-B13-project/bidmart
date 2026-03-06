package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.RoleRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.RoleSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoleJpaAdapter implements RoleRepositoryPort {
    private final RoleSpringRepository springRepository;

    @Override
    public Optional<Role> findByName(String name) {
        return springRepository.findByName(name);
    }

    @Override
    public Role save(Role role) {
        return springRepository.save(role);
    }
}


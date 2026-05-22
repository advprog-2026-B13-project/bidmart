package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.RoleRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.RoleSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoleJpaAdapter implements RoleRepositoryPort {
    private final RoleSpringRepository springRepository;

    @Override
    public Optional<Role> findById(int roleId) {
        return springRepository.findById(roleId);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return springRepository.findByName(name);
    }

    @Override
    public List<Role> findAll() {
        return springRepository.findAll();
    }

    @Override
    public Role save(Role role) {
        return springRepository.save(role);
    }

    @Override
    public void deleteById(int roleId) {
        springRepository.deleteById(roleId);
    }
}

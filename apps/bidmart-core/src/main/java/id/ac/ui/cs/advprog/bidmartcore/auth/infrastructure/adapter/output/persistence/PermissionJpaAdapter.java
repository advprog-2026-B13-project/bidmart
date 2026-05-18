package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Permission;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.PermissionRepositoryPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.PermissionSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PermissionJpaAdapter implements PermissionRepositoryPort {
    private final PermissionSpringRepository springRepository;

    @Override
    public Optional<Permission> findByName(PermissionValue permissionValue) {
        return springRepository.findByName(permissionValue);
    }

    @Override
    public List<Permission> findAll() {
        return springRepository.findAll();
    }
}


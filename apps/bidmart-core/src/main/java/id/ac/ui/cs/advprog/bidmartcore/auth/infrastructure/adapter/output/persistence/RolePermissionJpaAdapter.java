package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.RolePermission;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.RolePermissionPort;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.RolePermissionSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RolePermissionJpaAdapter implements RolePermissionPort {
    private final RolePermissionSpringRepository springRepository;

    @Override
    public Set<PermissionValue> findPermissionsByRoleId(int roleId) {
        return springRepository.findAllByRoleId(roleId).stream()
                .map(rp -> rp.getId().getPermission().getName())
                .collect(Collectors.toSet());
    }
}


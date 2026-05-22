package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Permission;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.enums.PermissionValue;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.PermissionSpringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionJpaAdapterTest {

    @Mock
    private PermissionSpringRepository springRepository;

    private PermissionJpaAdapter adapter;

    @BeforeEach
    void unit_setUp() {
        adapter = new PermissionJpaAdapter(springRepository);
    }

    @Test
    void unit_findByName_found() {
        Permission perm = new Permission();
        when(springRepository.findByName(PermissionValue.ADMIN)).thenReturn(Optional.of(perm));

        Optional<Permission> result = adapter.findByName(PermissionValue.ADMIN);

        assertTrue(result.isPresent());
        assertSame(perm, result.get());
    }

    @Test
    void unit_findByName_notFound() {
        when(springRepository.findByName(PermissionValue.ADMIN)).thenReturn(Optional.empty());

        Optional<Permission> result = adapter.findByName(PermissionValue.ADMIN);

        assertTrue(result.isEmpty());
    }

    @Test
    void unit_findAll() {
        Permission p1 = new Permission();
        Permission p2 = new Permission();
        when(springRepository.findAll()).thenReturn(List.of(p1, p2));

        List<Permission> result = adapter.findAll();

        assertEquals(2, result.size());
    }
}

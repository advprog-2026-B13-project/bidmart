package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Role;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.RoleSpringRepository;
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
class RoleJpaAdapterTest {

    @Mock
    private RoleSpringRepository springRepository;

    private RoleJpaAdapter adapter;

    @BeforeEach
    void unit_setUp() {
        adapter = new RoleJpaAdapter(springRepository);
    }

    @Test
    void unit_findById_found() {
        Role role = new Role();
        when(springRepository.findById(1)).thenReturn(Optional.of(role));

        Optional<Role> result = adapter.findById(1);

        assertTrue(result.isPresent());
        assertSame(role, result.get());
    }

    @Test
    void unit_findById_notFound() {
        when(springRepository.findById(99)).thenReturn(Optional.empty());

        Optional<Role> result = adapter.findById(99);

        assertTrue(result.isEmpty());
    }

    @Test
    void unit_findByName_found() {
        Role role = new Role();
        when(springRepository.findByName("USER")).thenReturn(Optional.of(role));

        Optional<Role> result = adapter.findByName("USER");

        assertTrue(result.isPresent());
    }

    @Test
    void unit_findByName_notFound() {
        when(springRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        Optional<Role> result = adapter.findByName("UNKNOWN");

        assertTrue(result.isEmpty());
    }

    @Test
    void unit_findAll() {
        Role r1 = new Role();
        Role r2 = new Role();
        when(springRepository.findAll()).thenReturn(List.of(r1, r2));

        List<Role> result = adapter.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void unit_save() {
        Role role = new Role();
        when(springRepository.save(role)).thenReturn(role);

        Role result = adapter.save(role);

        assertSame(role, result);
    }

    @Test
    void unit_deleteById() {
        adapter.deleteById(5);

        verify(springRepository).deleteById(5);
    }
}

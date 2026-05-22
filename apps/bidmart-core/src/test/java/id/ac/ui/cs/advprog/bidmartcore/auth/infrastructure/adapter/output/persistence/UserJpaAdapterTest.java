package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.User;
import id.ac.ui.cs.advprog.bidmartcore.auth.domain.port.output.UserRepositoryPort.UserPage;
import id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring.UserSpringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserJpaAdapterTest {

    @Mock
    private UserSpringRepository springRepository;

    private UserJpaAdapter adapter;

    @BeforeEach
    void unit_setUp() {
        adapter = new UserJpaAdapter(springRepository);
    }

    @Test
    void unit_save_delegatesToSpringRepository() {
        User user = new User();
        when(springRepository.save(user)).thenReturn(user);

        User result = adapter.save(user);

        assertSame(user, result);
        verify(springRepository).save(user);
    }

    @Test
    void unit_findById_found() {
        UUID id = UUID.randomUUID();
        User user = new User();
        when(springRepository.findById(id)).thenReturn(Optional.of(user));

        Optional<User> result = adapter.findById(id);

        assertTrue(result.isPresent());
        assertSame(user, result.get());
    }

    @Test
    void unit_findById_notFound() {
        UUID id = UUID.randomUUID();
        when(springRepository.findById(id)).thenReturn(Optional.empty());

        Optional<User> result = adapter.findById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    void unit_findByEmail_found() {
        User user = new User();
        when(springRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        Optional<User> result = adapter.findByEmail("test@test.com");

        assertTrue(result.isPresent());
    }

    @Test
    void unit_findByEmail_notFound() {
        when(springRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

        Optional<User> result = adapter.findByEmail("test@test.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void unit_existsByEmail_true() {
        when(springRepository.existsByEmail("test@test.com")).thenReturn(true);

        assertTrue(adapter.existsByEmail("test@test.com"));
    }

    @Test
    void unit_existsByEmail_false() {
        when(springRepository.existsByEmail("test@test.com")).thenReturn(false);

        assertFalse(adapter.existsByEmail("test@test.com"));
    }

    @Test
    void unit_countByRoleId() {
        when(springRepository.countByRoleId(1)).thenReturn(5L);

        assertEquals(5L, adapter.countByRoleId(1));
    }

    @Test
    void unit_findUsersPage_returnsMappedUserPage() {
        User user1 = new User();
        User user2 = new User();
        List<User> users = List.of(user1, user2);
        Page<User> page = new PageImpl<>(users);
        when(springRepository.findAll(any(Pageable.class))).thenReturn(page);

        UserPage result = adapter.findUsersPage(0, 10);

        assertEquals(2, result.users().size());
        assertEquals(0, result.page());
        assertEquals(2, result.totalElements());
        assertEquals(1, result.totalPages());
        assertFalse(result.hasNext());
        verify(springRepository).findAll(any(Pageable.class));
    }
}

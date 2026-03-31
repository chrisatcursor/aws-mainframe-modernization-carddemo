package com.carddemo.user;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void findAll_returnsPage() {
        User user = new User();
        user.setUserId("USER0001");
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> entityPage = new PageImpl<>(List.of(user), pageable, 42);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(entityPage);

        Page<User> result = userService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(42);
        assertThat(result.getContent().getFirst().getUserId()).isEqualTo("USER0001");
        verify(userRepository).findAll(pageable);
    }

    @Test
    void findByUserId_returnsUser() {
        User user = new User();
        user.setUserId("ADMIN001");
        when(userRepository.findByUserId("ADMIN001")).thenReturn(Optional.of(user));

        User result = userService.findByUserId("ADMIN001");

        assertThat(result.getUserId()).isEqualTo("ADMIN001");
    }

    @Test
    void findByUserId_missing_throws() {
        when(userRepository.findByUserId("NONE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUserId("NONE"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("NONE");
    }

    @Test
    void createUser_encodesPasswordAndSaves() {
        when(userRepository.findByUserId("NEWUSER1")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("raw")).thenReturn("ENC");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createUser("newuser1", "F", "L", "raw", "U");

        assertThat(result.getUserId()).isEqualTo("NEWUSER1");
        assertThat(result.getFirstName()).isEqualTo("F");
        assertThat(result.getLastName()).isEqualTo("L");
        assertThat(result.getUserType()).isEqualTo("U");
        assertThat(result.getPassword()).isEqualTo("ENC");

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("raw");
    }

    @Test
    void createUser_duplicate_throws() {
        User existing = new User();
        when(userRepository.findByUserId("DUPE0001")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.createUser("DUPE0001", "F", "L", "p", "U"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DUPE0001");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_blankPassword_doesNotReEncode() {
        User user = new User();
        user.setUserId("U1");
        user.setPassword("HASH");
        when(userRepository.findByUserId("U1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUser("U1", "Fn", "Ln", "   ", "A");

        verify(passwordEncoder, never()).encode(any());
        assertThat(user.getPassword()).isEqualTo("HASH");
        assertThat(user.getUserType()).isEqualTo("A");
    }

    @Test
    void updateUser_newPassword_encodes() {
        User user = new User();
        user.setUserId("U1");
        user.setPassword("OLD");
        when(userRepository.findByUserId("U1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newp")).thenReturn("NEWHASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUser("U1", "Fn", "Ln", "newp", "U");

        verify(passwordEncoder).encode(eq("newp"));
        assertThat(user.getPassword()).isEqualTo("NEWHASH");
    }

    @Test
    void deleteUser_removes() {
        User user = new User();
        user.setUserId("DEL00001");
        when(userRepository.findByUserId("DEL00001")).thenReturn(Optional.of(user));

        userService.deleteUser("DEL00001");

        verify(userRepository).delete(user);
    }
}

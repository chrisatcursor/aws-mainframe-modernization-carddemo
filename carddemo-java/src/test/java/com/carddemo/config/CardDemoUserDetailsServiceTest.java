package com.carddemo.config;

import com.carddemo.user.User;
import com.carddemo.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardDemoUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CardDemoUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CardDemoUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_regularUser_returnsUserRole() {
        User user = new User() {};
        user.setUserId("USER0001");
        user.setPassword("$2a$10$hashed");
        user.setUserType("U");

        when(userRepository.findByUserId("USER0001")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("user0001");

        assertThat(result.getUsername()).isEqualTo("USER0001");
        assertThat(result.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_adminUser_returnsAdminRole() {
        User user = new User() {};
        user.setUserId("ADMIN001");
        user.setPassword("$2a$10$hashed");
        user.setUserType("A");

        when(userRepository.findByUserId("ADMIN001")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("admin001");

        assertThat(result.getUsername()).isEqualTo("ADMIN001");
        assertThat(result.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_userNotFound_throwsException() {
        when(userRepository.findByUserId("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void loadUserByUsername_normalizesInput() {
        User user = new User() {};
        user.setUserId("USER0001");
        user.setPassword("$2a$10$hashed");
        user.setUserType("U");

        when(userRepository.findByUserId("USER0001")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("  user0001  ");
        assertThat(result.getUsername()).isEqualTo("USER0001");
    }
}

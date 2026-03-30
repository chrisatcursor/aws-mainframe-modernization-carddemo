package com.carddemo.config;

import com.carddemo.user.User;
import com.carddemo.user.UserRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Minimal UserDetailsService backed by the users table.
 * Phase 3 (COSGN00C migration) will replace this with a full implementation.
 */
@Service
public class StubUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public StubUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserId(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String role = user.isAdmin() ? "ROLE_ADMIN" : "ROLE_USER";
        return new org.springframework.security.core.userdetails.User(
                user.getUserId(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}

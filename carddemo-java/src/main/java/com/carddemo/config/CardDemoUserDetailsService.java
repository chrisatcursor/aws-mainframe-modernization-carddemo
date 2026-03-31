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
 * UserDetailsService backed by the users table (USRSEC VSAM file).
 * Migrated from COSGN00C READ-USER-SEC-FILE paragraph.
 * COBOL lookup: READ DATASET(USRSEC) INTO(SEC-USER-DATA) RIDFLD(WS-USER-ID).
 */
@Service
public class CardDemoUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CardDemoUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedId = username.toUpperCase().trim();

        User user = userRepository.findByUserId(normalizedId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + normalizedId));

        String role = user.isAdmin() ? "ROLE_ADMIN" : "ROLE_USER";
        return new org.springframework.security.core.userdetails.User(
                user.getUserId(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}

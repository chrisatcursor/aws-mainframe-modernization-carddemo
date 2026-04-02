package com.carddemo.user;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CardDemoUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CardDemoUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity u = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
        String role = "A".equalsIgnoreCase(u.getUserType()) ? "ROLE_ADMIN" : "ROLE_USER";
        return new User(u.getUsername(), u.getPassword(), List.of(new SimpleGrantedAuthority(role)));
    }
}

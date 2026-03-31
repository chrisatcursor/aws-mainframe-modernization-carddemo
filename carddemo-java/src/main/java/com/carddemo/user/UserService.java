package com.carddemo.user;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public User findByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    public User createUser(String userId, String firstName, String lastName,
            String password, String userType) {
        String normalizedId = userId.toUpperCase().trim();
        if (userRepository.findByUserId(normalizedId).isPresent()) {
            throw new IllegalArgumentException("User already exists: " + normalizedId);
        }
        User user = new User();
        user.setUserId(normalizedId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(passwordEncoder.encode(password));
        user.setUserType(userType);
        return userRepository.save(user);
    }

    public User updateUser(String userId, String firstName, String lastName,
            String password, String userType) {
        User user = findByUserId(userId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setUserType(userType);
        return userRepository.save(user);
    }

    public void deleteUser(String userId) {
        User user = findByUserId(userId);
        userRepository.delete(user);
    }
}

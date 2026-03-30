package com.carddemo.user;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindByUserId() {
        User user = new User();
        user.setUserId("TESTUS01");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPassword("$2a$10$hashedpassword");
        user.setUserType("U");

        userRepository.save(user);

        Optional<User> found = userRepository.findByUserId("TESTUS01");
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Test");
        assertThat(found.get().isAdmin()).isFalse();
    }

    @Test
    void findByUserId_notFound_returnsEmpty() {
        Optional<User> found = userRepository.findByUserId("NOUSER");
        assertThat(found).isEmpty();
    }

    @Test
    void adminUserType_isAdmin() {
        User admin = new User();
        admin.setUserId("ADMIN001");
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setPassword("$2a$10$hashedpassword");
        admin.setUserType("A");

        userRepository.save(admin);

        User found = userRepository.findByUserId("ADMIN001").orElseThrow();
        assertThat(found.isAdmin()).isTrue();
    }
}

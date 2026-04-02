package com.carddemo.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.user.UserEntity;
import com.carddemo.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedUsersAndAccounts(
            UserRepository userRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                UserEntity admin = new UserEntity();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("password"));
                admin.setUserType("A");
                userRepository.save(admin);
                UserEntity user = new UserEntity();
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode("password"));
                user.setUserType("U");
                userRepository.save(user);
            }
            if (accountRepository.count() == 0) {
                Account a = new Account();
                a.setAccountId(1L);
                a.setActiveStatus("A");
                a.setCurrentBalance(new BigDecimal("100.00"));
                a.setCreditLimit(new BigDecimal("5000.00"));
                a.setCashCreditLimit(new BigDecimal("500.00"));
                a.setOpenDate("01-01-2020");
                a.setExpirationDate("12-31-2030");
                a.setReissueDate("01-01-2024");
                a.setCurrentCycleCredit(BigDecimal.ZERO);
                a.setCurrentCycleDebit(BigDecimal.ZERO);
                a.setAddrZip("12345");
                a.setGroupId("GRP1");
                accountRepository.save(a);
            }
        };
    }
}

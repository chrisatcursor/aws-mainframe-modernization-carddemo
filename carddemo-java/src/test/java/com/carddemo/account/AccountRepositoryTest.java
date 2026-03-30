package com.carddemo.account;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void saveAndFindById() {
        Account account = new Account();
        account.setAcctId(10000000001L);
        account.setActiveStatus("Y");
        account.setCurrentBalance(new BigDecimal("1500.00"));
        account.setCreditLimit(new BigDecimal("5000.00"));
        account.setCashCreditLimit(new BigDecimal("1500.00"));
        account.setOpenDate("2020-01-15");
        account.setExpirationDate("2025-01-15");

        accountRepository.save(account);

        Optional<Account> found = accountRepository.findById(10000000001L);
        assertThat(found).isPresent();
        assertThat(found.get().getActiveStatus()).isEqualTo("Y");
        assertThat(found.get().getCurrentBalance()).isEqualByComparingTo("1500.00");
    }

    @Test
    void findById_notFound_returnsEmpty() {
        Optional<Account> found = accountRepository.findById(99999999999L);
        assertThat(found).isEmpty();
    }

    @Test
    void updateBalance() {
        Account account = new Account();
        account.setAcctId(20000000001L);
        account.setActiveStatus("Y");
        account.setCurrentBalance(new BigDecimal("1000.00"));
        accountRepository.save(account);

        account.setCurrentBalance(new BigDecimal("2000.00"));
        accountRepository.save(account);

        Account updated = accountRepository.findById(20000000001L).orElseThrow();
        assertThat(updated.getCurrentBalance()).isEqualByComparingTo("2000.00");
    }
}

package com.carddemo.behavior.account;

import com.carddemo.account.model.Account;
import com.carddemo.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 behavioral tests for legacy COACTUPC (account update).
 * Target: persisted changes to {@link Account} (limits, status, cycle totals, dates) via a future service.
 */
@SpringBootTest
@TestPropertySource(properties = "carddemo.bootstrap.enabled=true")
class CoactupcBehaviorTest {

    @Autowired
    private AccountRepository accountRepository;

    private Long accountId;

    @BeforeEach
    void pickAccount() {
        accountId = accountRepository.findAll().stream()
                .map(Account::getAccountId)
                .findFirst()
                .orElseThrow();
    }

    @Test
    @Transactional
    @Disabled("Wire AccountUpdateService (COACTUPC) then remove; act is TODO")
    void updateCreditLimit_persistsNewLimitAndLeavesBalanceUnchanged_whenRequestValid() {
        Account before = accountRepository.findById(accountId).orElseThrow();
        BigDecimal priorBalance = before.getCurrentBalance();

        // TODO: accountUpdateService.updateCreditLimit(accountId, new BigDecimal("7500.00"));

        Account after = accountRepository.findById(accountId).orElseThrow();
        assertThat(after.getCreditLimit()).isEqualByComparingTo("7500.00");
        assertThat(after.getCurrentBalance()).isEqualByComparingTo(priorBalance);
    }

    @Test
    @Transactional
    @Disabled("Wire AccountUpdateService (COACTUPC) then remove; act is TODO")
    void updateActiveStatus_flipsAccountRow_whenAuthorized() {
        // TODO: accountUpdateService.setActiveStatus(accountId, "N");

        Account after = accountRepository.findById(accountId).orElseThrow();
        assertThat(after.getActiveStatus()).isEqualTo("N");
    }
}

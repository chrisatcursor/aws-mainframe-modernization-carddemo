package com.carddemo.behavior.account;

import com.carddemo.account.api.AccountUpdateRequest;
import com.carddemo.account.model.Account;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.account.service.AccountService;
import com.carddemo.card.model.CardXref;
import com.carddemo.card.repository.CardXrefRepository;
import com.carddemo.common.error.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 2 behavioral tests for legacy COACTUPC (account update).
 * Target: persisted changes to {@link Account} (limits, status, cycle totals, dates) via a future service.
 */
@SpringBootTest
@TestPropertySource(properties = "carddemo.bootstrap.enabled=true")
class CoactupcBehaviorTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private AccountService accountService;

    private Long accountId;
    private Long customerId;

    @BeforeEach
    void pickAccount() {
        CardXref xref = cardXrefRepository.findAll().stream()
                .findFirst()
                .orElseThrow();
        accountId = xref.getAccountId();
        customerId = xref.getCustomerId();
    }

    @Test
    @Transactional
    void updateCreditLimit_persistsNewLimitAndLeavesBalanceUnchanged_whenRequestValid() {
        Account before = accountRepository.findById(accountId).orElseThrow();
        BigDecimal priorCreditLimit = before.getCreditLimit();
        BigDecimal priorBalance = before.getCurrentBalance();
        long priorVersion = before.getVersion() == null ? 0L : before.getVersion();

        var view = accountService.getAccountView(accountId);
        AccountUpdateRequest request = new AccountUpdateRequest(
                before.getActiveStatus(),
                before.getCreditLimit().add(new BigDecimal("250.00")),
                before.getCashCreditLimit().add(new BigDecimal("100.00")),
                before.getExpirationDate(),
                before.getReissueDate(),
                view.firstName(),
                view.middleName(),
                view.lastName(),
                pad(view.addressLine1(), 50),
                pad(view.addressLine2(), 50),
                pad(view.addressLine3(), 50),
                view.stateCode(),
                view.countryCode(),
                pad(view.zip(), 10),
                pad(view.phone1(), 15),
                pad(view.phone2(), 15),
                "123456789",
                view.dateOfBirth() == null ? LocalDate.of(1980, 1, 1) : view.dateOfBirth(),
                view.ficoScore() == null ? 700 : view.ficoScore(),
                priorVersion
        );

        accountService.updateAccount(accountId, request);

        Account after = accountRepository.findById(accountId).orElseThrow();
        assertThat(after.getCreditLimit()).isEqualByComparingTo(priorCreditLimit.add(new BigDecimal("250.00")));
        assertThat(after.getCurrentBalance()).isEqualByComparingTo(priorBalance);
    }

    @Test
    @Transactional
    void updateActiveStatus_rejectsInvalidSsnSegment() {
        Account before = accountRepository.findById(accountId).orElseThrow();
        var view = accountService.getAccountView(accountId);
        AccountUpdateRequest request = new AccountUpdateRequest(
                before.getActiveStatus(),
                before.getCreditLimit(),
                before.getCashCreditLimit(),
                before.getExpirationDate(),
                before.getReissueDate(),
                view.firstName(),
                view.middleName(),
                view.lastName(),
                pad(view.addressLine1(), 50),
                pad(view.addressLine2(), 50),
                pad(view.addressLine3(), 50),
                view.stateCode(),
                view.countryCode(),
                pad(view.zip(), 10),
                pad(view.phone1(), 15),
                pad(view.phone2(), 15),
                "931248469",
                view.dateOfBirth() == null ? LocalDate.of(1980, 1, 1) : view.dateOfBirth(),
                view.ficoScore() == null ? 700 : view.ficoScore(),
                before.getVersion()
        );

        assertThatThrownBy(() -> accountService.updateAccount(accountId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("invalid first segment");
    }

    private static String pad(String value, int len) {
        String src = value == null ? "" : value.trim();
        if (src.length() >= len) {
            return src.substring(0, len);
        }
        return src + " ".repeat(len - src.length());
    }
}

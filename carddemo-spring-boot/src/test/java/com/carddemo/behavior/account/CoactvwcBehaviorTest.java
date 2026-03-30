package com.carddemo.behavior.account;

import com.carddemo.account.model.Account;
import com.carddemo.account.model.Customer;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.account.repository.CustomerRepository;
import com.carddemo.account.service.AccountService;
import com.carddemo.card.model.CardXref;
import com.carddemo.card.repository.CardXrefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 behavioral tests for legacy COACTVWC (account view).
 * Target: read-only assembly of {@link Account}, {@link Customer}, and card linkage via {@link CardXref}.
 */
@SpringBootTest
@TestPropertySource(properties = "carddemo.bootstrap.enabled=true")
@Tag("phase2")
class CoactvwcBehaviorTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private AccountService accountService;

    private Long sampleAccountId;

    @BeforeEach
    void pickAccountWithXrefs() {
        sampleAccountId = cardXrefRepository.findAll().stream()
                .mapToLong(CardXref::getAccountId)
                .distinct()
                .findFirst()
                .orElseThrow();
    }

    @Test
    void viewAccountDetails_returnsAccountWithFinancialFieldsPopulated() {
        var view = accountService.getAccountView(sampleAccountId);
        Account account = accountRepository.findById(sampleAccountId).orElseThrow();

        assertThat(account.getActiveStatus()).isNotBlank();
        assertThat(account.getCurrentBalance()).isNotNull();
        assertThat(account.getCreditLimit()).isNotNull();
        assertThat(account.getCashCreditLimit()).isNotNull();
        assertThat(account.getCurrentCycleCredit()).isNotNull();
        assertThat(account.getCurrentCycleDebit()).isNotNull();

        assertThat(view.accountId()).isEqualTo(account.getAccountId());
        assertThat(view.currentBalance()).isEqualByComparingTo(account.getCurrentBalance());
        assertThat(view.creditLimit()).isEqualByComparingTo(account.getCreditLimit());
    }

    @Test
    void viewAccountDetails_resolvesCustomersLinkedViaCardXref() {
        List<CardXref> xrefs = cardXrefRepository.findByAccountIdOrderByCardNumberAsc(sampleAccountId);
        assertThat(xrefs).isNotEmpty();

        var view = accountService.getAccountView(sampleAccountId);
        for (CardXref xref : xrefs) {
            Customer customer = customerRepository.findById(xref.getCustomerId()).orElseThrow();
            assertThat(customer.getCustomerId()).isEqualTo(xref.getCustomerId());
            assertThat(customer.getCustomerId()).isPositive();
        }
        assertThat(view.customerId()).isEqualTo(xrefs.getFirst().getCustomerId());
        assertThat(view.firstName()).isNotBlank();
    }
}

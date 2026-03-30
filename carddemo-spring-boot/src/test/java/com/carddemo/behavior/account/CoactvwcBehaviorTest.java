package com.carddemo.behavior.account;

import com.carddemo.account.model.Account;
import com.carddemo.account.model.Customer;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.account.repository.CustomerRepository;
import com.carddemo.card.model.CardXref;
import com.carddemo.card.repository.CardXrefRepository;
import org.junit.jupiter.api.BeforeEach;
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
class CoactvwcBehaviorTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

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
        // TODO: AccountViewService.getAccountDetails(sampleAccountId) — assert DTO maps these fields
        Account account = accountRepository.findById(sampleAccountId).orElseThrow();

        assertThat(account.getActiveStatus()).isNotBlank();
        assertThat(account.getCurrentBalance()).isNotNull();
        assertThat(account.getCreditLimit()).isNotNull();
        assertThat(account.getCashCreditLimit()).isNotNull();
        assertThat(account.getCurrentCycleCredit()).isNotNull();
        assertThat(account.getCurrentCycleDebit()).isNotNull();
    }

    @Test
    void viewAccountDetails_resolvesCustomersLinkedViaCardXref() {
        List<CardXref> xrefs = cardXrefRepository.findByAccountIdOrderByCardNumberAsc(sampleAccountId);
        assertThat(xrefs).isNotEmpty();

        for (CardXref xref : xrefs) {
            // TODO: service should expose same customer join as legacy screen
            Customer customer = customerRepository.findById(xref.getCustomerId()).orElseThrow();
            assertThat(customer.getCustomerId()).isEqualTo(xref.getCustomerId());
            assertThat(customer.getCustomerId()).isPositive();
        }
    }
}

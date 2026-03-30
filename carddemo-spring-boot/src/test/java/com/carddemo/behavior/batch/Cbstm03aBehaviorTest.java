package com.carddemo.behavior.batch;

import com.carddemo.account.model.Account;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.transaction.model.DisclosureGroup;
import com.carddemo.transaction.model.DisclosureGroupId;
import com.carddemo.transaction.repository.DisclosureGroupRepository;
import com.carddemo.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 behavioral tests for legacy CBSTM03A (statement generation).
 * Target: ordered {@link com.carddemo.transaction.model.TransactionRecord} history per card and disclosure/interest rows via {@link DisclosureGroup}.
 */
@SpringBootTest
@TestPropertySource(properties = "carddemo.bootstrap.enabled=true")
class Cbstm03aBehaviorTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DisclosureGroupRepository disclosureGroupRepository;

    @Test
    void statementDataSources_existForSampleAccountGroup() {
        assertThat(disclosureGroupRepository.count()).isPositive();
        DisclosureGroup sample = disclosureGroupRepository.findAll().stream().findFirst().orElseThrow();
        assertThat(sample.getId()).isNotNull();
        assertThat(sample.getInterestRate()).isNotNull();

        accountRepository.findAll().stream()
                .filter(a -> a.getGroupId() != null && !a.getGroupId().isBlank())
                .findFirst()
                .flatMap(account -> disclosureGroupRepository.findById(
                        new DisclosureGroupId(account.getGroupId(), "01", 1)))
                .ifPresent(dg -> assertThat(dg.getInterestRate()).isNotNull());
    }

    @Test
    @Transactional
    @Disabled("Wire StatementGenerationService (CBSTM03A) then remove; act is TODO")
    void generateStatement_includesRecentTransactionsAndDisclosureRate_forAccount() {
        Account account = accountRepository.findAll().stream()
                .filter(a -> a.getGroupId() != null && !a.getGroupId().isBlank())
                .findFirst()
                .orElseThrow();
        String cardNumber = transactionRepository.findAll().stream()
                .map(com.carddemo.transaction.model.TransactionRecord::getCardNumber)
                .findFirst()
                .orElseThrow();

        // TODO: statementService.buildStatement(account.getAccountId(), cardNumber, /* statement period */);

        var page = transactionRepository.findByCardNumberOrderByProcessedTimestampDesc(cardNumber, PageRequest.of(0, 5));
        assertThat(page.getContent()).isNotEmpty();

        DisclosureGroup dg = disclosureGroupRepository
                .findByIdAccountGroupIdAndIdTransactionTypeCodeAndIdTransactionCategoryCode(
                        account.getGroupId(), "01", 1)
                .orElseGet(() -> disclosureGroupRepository.findAll().iterator().next());
        assertThat(dg.getInterestRate()).isNotNull();
    }
}

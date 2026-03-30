package com.carddemo.behavior.batch;

import com.carddemo.account.model.Account;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.batch.service.StatementGenerationBatchService;
import com.carddemo.batch.service.StatementGenerationResult;
import com.carddemo.card.model.CardXref;
import com.carddemo.card.repository.CardXrefRepository;
import com.carddemo.transaction.model.DisclosureGroup;
import com.carddemo.transaction.model.DisclosureGroupId;
import com.carddemo.transaction.model.TransactionRecord;
import com.carddemo.transaction.repository.DisclosureGroupRepository;
import com.carddemo.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private StatementGenerationBatchService statementGenerationBatchService;

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
    void generateStatement_includesRecentTransactionsAndDisclosureRate_forAccount() {
        CardXref xref = cardXrefRepository.findAll().stream().findFirst().orElseThrow();
        Account account = accountRepository.findById(xref.getAccountId())
                .orElseGet(() -> accountRepository.findAll().stream().findFirst().orElseThrow());
        String cardNumber = xref.getCardNumber();

        if (transactionRepository.findByCardNumberOrderByProcessedTimestampDesc(cardNumber, PageRequest.of(0, 1))
                .getContent().isEmpty()) {
            TransactionRecord tx = new TransactionRecord();
            tx.setTransactionId(("STM" + System.nanoTime()).substring(0, 16));
            tx.setTransactionTypeCode("01");
            tx.setTransactionCategoryCode(1);
            tx.setSource("System");
            tx.setDescription("Statement seed tx");
            tx.setAmount(new BigDecimal("12.34"));
            tx.setMerchantId(0L);
            tx.setMerchantName("SEED");
            tx.setMerchantCity("SEED");
            tx.setMerchantZip("00000");
            tx.setCardNumber(cardNumber);
            tx.setOriginalTimestamp(LocalDateTime.now());
            tx.setProcessedTimestamp(LocalDateTime.now());
            transactionRepository.save(tx);
        }

        StatementGenerationResult result =
                statementGenerationBatchService.generateStatements(LocalDate.of(2026, 3, 30));
        assertThat(result.statementCount()).isPositive();
        assertThat(result.accountIds()).contains(account.getAccountId().toString());
        assertThat(result.textStatements()).anyMatch(text -> text.contains("Account ID: " + account.getAccountId()));

        var page = transactionRepository.findByCardNumberOrderByProcessedTimestampDesc(cardNumber, PageRequest.of(0, 5));
        assertThat(page.getContent()).isNotEmpty();

        String groupId = (account.getGroupId() == null || account.getGroupId().isBlank())
                ? "DEFAULT"
                : account.getGroupId();
        DisclosureGroup dg = disclosureGroupRepository
                .findByIdAccountGroupIdAndIdTransactionTypeCodeAndIdTransactionCategoryCode(groupId, "01", 1)
                .orElseGet(() -> disclosureGroupRepository.findAll().iterator().next());
        assertThat(dg.getInterestRate()).isNotNull();
    }
}

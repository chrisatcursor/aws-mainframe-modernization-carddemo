package com.carddemo.batch;

import com.carddemo.account.model.Account;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.batch.service.InterestCalculationBatchResult;
import com.carddemo.batch.service.InterestCalculationBatchService;
import com.carddemo.card.model.CardXref;
import com.carddemo.card.repository.CardXrefRepository;
import com.carddemo.transaction.model.DisclosureGroup;
import com.carddemo.transaction.model.DisclosureGroupId;
import com.carddemo.transaction.model.TransactionCategoryBalance;
import com.carddemo.transaction.model.TransactionCategoryBalanceId;
import com.carddemo.transaction.repository.DisclosureGroupRepository;
import com.carddemo.transaction.repository.TransactionCategoryBalanceRepository;
import com.carddemo.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "carddemo.bootstrap.enabled=true")
class InterestCalculationBatchServiceTest {

    @Autowired
    private InterestCalculationBatchService service;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionCategoryBalanceRepository balanceRepository;

    @Autowired
    private DisclosureGroupRepository disclosureGroupRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void calculateInterest_addsInterestTransactionsAndUpdatesAccount() {
        Account account = accountRepository.findAll().stream().findFirst().orElseThrow();
        CardXref xref = cardXrefRepository.findByAccountIdOrderByCardNumberAsc(account.getAccountId()).stream().findFirst().orElseThrow();

        TransactionCategoryBalanceId id = new TransactionCategoryBalanceId(account.getAccountId(), "01", 1);
        TransactionCategoryBalance bal = balanceRepository.findById(id).orElseGet(() -> {
            TransactionCategoryBalance created = new TransactionCategoryBalance();
            created.setId(id);
            created.setBalance(new BigDecimal("1000.00"));
            return balanceRepository.save(created);
        });
        bal.setBalance(new BigDecimal("1000.00"));
        balanceRepository.save(bal);

        DisclosureGroupId dgId = new DisclosureGroupId(account.getGroupId(), "01", 1);
        DisclosureGroup dg = disclosureGroupRepository.findById(dgId).orElseGet(() -> {
            DisclosureGroup created = new DisclosureGroup();
            created.setId(dgId);
            created.setInterestRate(new BigDecimal("120.00"));
            return disclosureGroupRepository.save(created);
        });
        dg.setInterestRate(new BigDecimal("120.00"));
        disclosureGroupRepository.save(dg);

        long txBefore = transactionRepository.count();
        BigDecimal balBefore = account.getCurrentBalance();

        InterestCalculationBatchResult result = service.calculateInterest("2026-07-18");

        assertThat(result.generatedTransactions()).isGreaterThan(0);
        assertThat(transactionRepository.count()).isGreaterThan(txBefore);
        assertThat(result.nextTransactionSuffix()).isGreaterThan(0);

        Account refreshed = accountRepository.findById(account.getAccountId()).orElseThrow();
        assertThat(refreshed.getCurrentBalance()).isGreaterThanOrEqualTo(balBefore);
        assertThat(transactionRepository.findByCardNumberOrderByProcessedTimestampAsc(xref.getCardNumber()))
                .isNotEmpty();
    }
}

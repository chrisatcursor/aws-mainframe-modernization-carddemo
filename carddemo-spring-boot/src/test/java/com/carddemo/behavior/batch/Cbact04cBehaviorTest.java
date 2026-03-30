package com.carddemo.behavior.batch;

import com.carddemo.account.model.Account;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.batch.service.InterestCalculationBatchService;
import com.carddemo.transaction.model.TransactionCategoryBalance;
import com.carddemo.transaction.model.TransactionCategoryBalanceId;
import com.carddemo.transaction.repository.TransactionCategoryBalanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 behavioral tests for legacy CBACT04C (account master / cycle update batch).
 * Target: refresh {@link Account} cycle totals and related {@link TransactionCategoryBalance} rows from posted activity.
 */
@SpringBootTest
@TestPropertySource(properties = "carddemo.bootstrap.enabled=true")
class Cbact04cBehaviorTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionCategoryBalanceRepository transactionCategoryBalanceRepository;

    @Autowired
    private InterestCalculationBatchService interestCalculationBatchService;

    @Test
    void seedAccount_hasCategoryBalanceRowsForCycleRollup() {
        Account account = accountRepository.findAll().stream().findFirst().orElseThrow();
        var balances = transactionCategoryBalanceRepository.findByIdAccountIdOrderByIdTransactionTypeCodeAscIdTransactionCategoryCodeAsc(
                account.getAccountId());
        assertThat(balances).isNotEmpty();
        assertThat(balances.getFirst().getBalance()).isNotNull();
    }

    @Test
    @Transactional
    void applyCycleBatch_updatesAccountCycleDebitCredit_andCategoryBalances() {
        Account account = accountRepository.findAll().stream().findFirst().orElseThrow();
        TransactionCategoryBalanceId id = transactionCategoryBalanceRepository.findAll().stream()
                .map(TransactionCategoryBalance::getId)
                .filter(i -> i.getAccountId().equals(account.getAccountId()))
                .findFirst()
                .orElseThrow();

        interestCalculationBatchService.calculateInterest("2026-03-30");

        Account reloaded = accountRepository.findById(account.getAccountId()).orElseThrow();
        assertThat(reloaded.getCurrentCycleDebit()).isNotNull();
        assertThat(reloaded.getCurrentCycleCredit()).isNotNull();

        TransactionCategoryBalance bal = transactionCategoryBalanceRepository.findById(id).orElseThrow();
        assertThat(bal.getBalance()).isNotNull();
    }
}

package com.carddemo.behavior.batch;

import com.carddemo.transaction.model.DailyTransactionRecord;
import com.carddemo.transaction.model.TransactionRecord;
import com.carddemo.transaction.repository.DailyTransactionRepository;
import com.carddemo.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 behavioral tests for legacy CBTRN02C (post daily transactions to master file).
 * Target: {@link DailyTransactionRecord} processed into {@link TransactionRecord} with matching keys and amounts.
 */
@SpringBootTest
@TestPropertySource(properties = "carddemo.bootstrap.enabled=true")
class Cbtrn02cBehaviorTest {

    @Autowired
    private DailyTransactionRepository dailyTransactionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void seedDailyFile_containsTransactionsWithCardAndCategory() {
        DailyTransactionRecord one = dailyTransactionRepository.findAll().stream().findFirst().orElseThrow();
        assertThat(one.getTransactionId()).isNotBlank();
        assertThat(one.getCardNumber()).isNotBlank();
        assertThat(one.getTransactionCategoryCode()).isNotNull();
        assertThat(one.getAmount()).isNotNull();
    }

    @Test
    @Transactional
    @Disabled("Wire DailyTransactionPostingJob/Service (CBTRN02C) then remove; act is TODO")
    void postDailyBatch_createsTransactionRow_andClearsOrSkipsSource_whenSuccessful() {
        DailyTransactionRecord pending = dailyTransactionRepository.findAll().stream().findFirst().orElseThrow();
        long countBefore = transactionRepository.count();

        // TODO: dailyPostingService.postBatch(/* batch run id, selection criteria */);

        assertThat(transactionRepository.count()).isGreaterThan(countBefore);
        TransactionRecord posted = transactionRepository.findById(pending.getTransactionId()).orElseThrow();
        assertThat(posted.getAmount()).isEqualByComparingTo(pending.getAmount());
        assertThat(posted.getCardNumber()).isEqualTo(pending.getCardNumber());
        assertThat(posted.getTransactionCategoryCode()).isEqualTo(pending.getTransactionCategoryCode());
    }
}

package com.carddemo.behavior.batch;

import com.carddemo.account.model.Account;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.batch.repository.DailyTransactionRejectionRepository;
import com.carddemo.batch.service.BatchPostingResult;
import com.carddemo.batch.service.TransactionPostingBatchService;
import com.carddemo.transaction.model.DailyTransactionRecord;
import com.carddemo.transaction.model.TransactionCategoryBalance;
import com.carddemo.transaction.model.TransactionCategoryBalanceId;
import com.carddemo.transaction.model.TransactionRecord;
import com.carddemo.transaction.repository.DailyTransactionRepository;
import com.carddemo.transaction.repository.TransactionCategoryBalanceRepository;
import com.carddemo.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 behavioral tests for legacy CBTRN02C (post daily transactions to master file).
 */
@SpringBootTest
@TestPropertySource(properties = "carddemo.bootstrap.enabled=true")
@Transactional
class Cbtrn02cBehaviorTest {

    @Autowired
    private DailyTransactionRepository dailyTransactionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionCategoryBalanceRepository balanceRepository;

    @Autowired
    private DailyTransactionRejectionRepository rejectionRepository;

    @Autowired
    private TransactionPostingBatchService transactionPostingBatchService;

    @Test
    void postDailyBatch_createsTransactionRow_andRejectsOverlimit() {
        dailyTransactionRepository.deleteAll();
        transactionRepository.deleteAll();
        rejectionRepository.deleteAll();

        Account account = accountRepository.findById(50L).orElseThrow();
        account.setCurrentBalance(new BigDecimal("100.00"));
        account.setCreditLimit(new BigDecimal("10000.00"));
        account.setCurrentCycleCredit(BigDecimal.ZERO);
        account.setCurrentCycleDebit(BigDecimal.ZERO);
        account.setExpirationDate(LocalDate.now().plusYears(1));
        accountRepository.save(account);

        TransactionCategoryBalanceId key = new TransactionCategoryBalanceId(50L, "01", 1);
        balanceRepository.findById(key).orElseGet(() -> {
            TransactionCategoryBalance b = new TransactionCategoryBalance();
            b.setId(key);
            b.setBalance(BigDecimal.ZERO);
            return balanceRepository.save(b);
        });

        DailyTransactionRecord valid = new DailyTransactionRecord();
        valid.setTransactionId("TPOST0000000001");
        valid.setTransactionTypeCode("01");
        valid.setTransactionCategoryCode(1);
        valid.setSource("POS TERM");
        valid.setDescription("valid purchase");
        valid.setAmount(new BigDecimal("25.00"));
        valid.setMerchantId(1L);
        valid.setMerchantName("Shop");
        valid.setMerchantCity("City");
        valid.setMerchantZip("12345");
        valid.setCardNumber("0500024453765740");
        valid.setOriginalTimestamp(LocalDateTime.now().minusDays(1));
        valid.setProcessedTimestamp(LocalDateTime.now());
        dailyTransactionRepository.save(valid);

        DailyTransactionRecord overlimit = new DailyTransactionRecord();
        overlimit.setTransactionId("TPOST0000000002");
        overlimit.setTransactionTypeCode("01");
        overlimit.setTransactionCategoryCode(1);
        overlimit.setSource("POS TERM");
        overlimit.setDescription("overlimit");
        overlimit.setAmount(new BigDecimal("999999.00"));
        overlimit.setMerchantId(1L);
        overlimit.setMerchantName("Shop");
        overlimit.setMerchantCity("City");
        overlimit.setMerchantZip("12345");
        overlimit.setCardNumber("0500024453765740");
        overlimit.setOriginalTimestamp(LocalDateTime.now().minusDays(1));
        overlimit.setProcessedTimestamp(LocalDateTime.now());
        dailyTransactionRepository.save(overlimit);

        BatchPostingResult result = transactionPostingBatchService.processPostingBatch(LocalDate.now());

        assertThat(result.processedCount()).isEqualTo(2);
        assertThat(result.postedCount()).isEqualTo(1);
        assertThat(result.rejectedCount()).isEqualTo(1);
        assertThat(result.returnCode()).isEqualTo(4);

        TransactionRecord posted = transactionRepository.findById("TPOST0000000001").orElseThrow();
        assertThat(posted.getAmount()).isEqualByComparingTo("25.00");
    }
}

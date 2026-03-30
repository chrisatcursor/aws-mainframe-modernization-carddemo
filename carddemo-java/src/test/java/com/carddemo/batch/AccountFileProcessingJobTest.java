package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.account.AccountTestFactory;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class AccountFileProcessingJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("accountFileProcessingJob")
    private Job accountFileProcessingJob;

    @Autowired
    private AccountRepository accountRepository;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        jobLauncherTestUtils.setJob(accountFileProcessingJob);
    }

    @Test
    void accountFileProcessingJob_completesSuccessfully_whenAccountsTableIsEmpty() throws Exception {
        JobExecution execution =
                jobLauncherTestUtils.launchJob(jobParams(tempDir).toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = AccountJobAssertions.processingStep(execution);
        assertThat(step.getWriteCount()).isZero();

        assertThat(Files.exists(tempDir.resolve(AccountFileProcessingJobConfig.OUTFILE_NAME)))
                .isTrue();
        assertThat(Files.exists(tempDir.resolve(AccountFileProcessingJobConfig.ARRYFILE_NAME)))
                .isTrue();
        assertThat(Files.exists(tempDir.resolve(AccountFileProcessingJobConfig.VBRCFILE_NAME)))
                .isTrue();
        assertThat(Files.readAllLines(tempDir.resolve(AccountFileProcessingJobConfig.OUTFILE_NAME)))
                .isEmpty();
    }

    @Test
    void accountFileProcessingJob_writesThreeFiles_withExpectedCountsAndFixedWidths() throws Exception {
        accountRepository.save(sampleAccount());

        JobExecution execution =
                jobLauncherTestUtils.launchJob(jobParams(tempDir).toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = AccountJobAssertions.processingStep(execution);
        assertThat(step.getWriteCount()).isEqualTo(1);

        List<String> outLines = Files.readAllLines(tempDir.resolve(AccountFileProcessingJobConfig.OUTFILE_NAME));
        List<String> arryLines = Files.readAllLines(tempDir.resolve(AccountFileProcessingJobConfig.ARRYFILE_NAME));
        List<String> vbrcLines = Files.readAllLines(tempDir.resolve(AccountFileProcessingJobConfig.VBRCFILE_NAME));

        assertThat(outLines).hasSize(1);
        assertThat(arryLines).hasSize(1);
        assertThat(vbrcLines).hasSize(2);

        String fixed = outLines.getFirst();
        assertThat(fixed.length()).isEqualTo(AccountLineAggregator.FIXED_WIDTH_RECORD_LENGTH);
        assertThat(fixed.substring(0, AccountLineAggregator.FIELD_ACCT_ID_LEN))
                .isEqualTo("00000001234");
        assertThat(fixed.substring(11, 12)).isEqualTo("Y");
        assertThat(fixed.substring(12, 12 + AccountLineAggregator.FIELD_MONEY_LEN))
                .isEqualTo(AccountLineAggregator.formatImpliedDecimalMoney(new BigDecimal("100.50")));
        assertThat(fixed.substring(90, 90 + AccountLineAggregator.FIELD_MONEY_LEN))
                .isEqualTo(AccountLineAggregator.formatImpliedDecimalMoney(new BigDecimal("2525.00")));

        assertThat(arryLines.getFirst()).contains("|1005.00|").contains("|1525.00|").contains("|-2500.00|");

        assertThat(vbrcLines.get(0)).isEqualTo("00000001234,Y");
        assertThat(vbrcLines.get(1)).contains("00000001234").contains("100.5").contains("5000");
    }

    @Test
    void accountFileProcessingJob_processesMultipleAccounts_orderedByAcctId() throws Exception {
        Account a = sampleAccount();
        a.setAcctId(999L);
        accountRepository.save(a);
        Account b = sampleAccount();
        b.setAcctId(100L);
        accountRepository.save(b);

        JobExecution execution =
                jobLauncherTestUtils.launchJob(jobParams(tempDir).toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(AccountJobAssertions.processingStep(execution).getWriteCount()).isEqualTo(2);

        List<String> outLines = Files.readAllLines(tempDir.resolve(AccountFileProcessingJobConfig.OUTFILE_NAME));
        assertThat(outLines).hasSize(2);
        assertThat(outLines.get(0).substring(0, 11)).isEqualTo("00000000100");
        assertThat(outLines.get(1).substring(0, 11)).isEqualTo("00000000999");
    }

    private static JobParametersBuilder jobParams(Path dir) {
        return new JobParametersBuilder()
                .addString(AccountFileProcessingJobConfig.PARAM_OUTPUT_DIR, dir.toAbsolutePath().toString())
                .addLong("run.id", System.nanoTime(), true);
    }

    /**
     * Nested so {@link org.springframework.batch.test.StepScopeTestExecutionListener} does not treat
     * a {@code StepExecution}-returning method on the test class as a step-scoped factory.
     */
    private static final class AccountJobAssertions {
        static StepExecution processingStep(JobExecution execution) {
            return execution.getStepExecutions().stream()
                    .filter(s -> AccountFileProcessingJobConfig.STEP_NAME.equals(s.getStepName()))
                    .findFirst()
                    .orElseThrow();
        }

        private AccountJobAssertions() {}
    }

    private static Account sampleAccount() {
        Account a = AccountTestFactory.newAccount();
        a.setAcctId(1234L);
        a.setActiveStatus("Y");
        a.setCurrentBalance(new BigDecimal("100.50"));
        a.setCreditLimit(new BigDecimal("5000.00"));
        a.setCashCreditLimit(new BigDecimal("1000.00"));
        a.setOpenDate("20240101");
        a.setExpirationDate("20261231");
        a.setReissueDate("20250315");
        a.setCurrentCycleCredit(new BigDecimal("50.00"));
        a.setCurrentCycleDebit(BigDecimal.ZERO);
        a.setGroupId("GRP1");
        return a;
    }
}

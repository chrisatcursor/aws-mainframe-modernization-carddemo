package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.account.AccountTestFactory;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class DailyTransactionValidationJobTest {

    private static final String CARD_OK = "4111111111111111";
    private static final String CARD_UNKNOWN = "4999999999999999";
    private static final String CARD_ORPHAN_XREF = "4222222222222222";
    private static final long ACCT_OK = 910_000_000_01L;
    private static final long ACCT_MISSING = 920_000_000_02L;

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("dailyTransactionValidationJob")
    private Job dailyTransactionValidationJob;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        cardXrefRepository.deleteAll();
        accountRepository.deleteAll();
        jobLauncherTestUtils.setJob(dailyTransactionValidationJob);
    }

    @Test
    void job_completes_whenValidXrefAndAccount() throws Exception {
        seedAccount(ACCT_OK);
        cardXrefRepository.save(CardXref.of(CARD_OK, 1L, ACCT_OK));

        Path file = writeTempDailyTranFile(dailyTranLine("TX-VALID-00000001", CARD_OK));

        JobExecution execution = StepAssertions.runJob(jobLauncherTestUtils, file);
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = StepAssertions.validationStep(execution);
        assertThat(step.getReadCount()).isEqualTo(1);
        assertThat(step.getWriteCount()).isEqualTo(1);
        assertThat(step.getFilterCount()).isZero();
    }

    @Test
    void job_filtersRow_whenNoCardXref() throws Exception {
        seedAccount(ACCT_OK);

        Path file = writeTempDailyTranFile(dailyTranLine("TX-BAD-XREF-000001", CARD_UNKNOWN));

        JobExecution execution = StepAssertions.runJob(jobLauncherTestUtils, file);
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = StepAssertions.validationStep(execution);
        assertThat(step.getReadCount()).isEqualTo(1);
        assertThat(step.getWriteCount()).isZero();
        assertThat(step.getFilterCount()).isEqualTo(1);
    }

    @Test
    void job_filtersRow_whenXrefPointsToMissingAccount() throws Exception {
        cardXrefRepository.save(CardXref.of(CARD_ORPHAN_XREF, 1L, ACCT_MISSING));

        Path file = writeTempDailyTranFile(dailyTranLine("TX-NO-ACCT-0000001", CARD_ORPHAN_XREF));

        JobExecution execution = StepAssertions.runJob(jobLauncherTestUtils, file);
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = StepAssertions.validationStep(execution);
        assertThat(step.getReadCount()).isEqualTo(1);
        assertThat(step.getWriteCount()).isZero();
        assertThat(step.getFilterCount()).isEqualTo(1);
    }

    @Test
    void job_mixedInput_countsReadsWritesAndFilters() throws Exception {
        seedAccount(ACCT_OK);
        cardXrefRepository.save(CardXref.of(CARD_OK, 1L, ACCT_OK));
        cardXrefRepository.save(CardXref.of(CARD_ORPHAN_XREF, 1L, ACCT_MISSING));

        Path file = writeTempDailyTranFile(
                dailyTranLine("TX-001-VALID------", CARD_OK),
                dailyTranLine("TX-002-BAD-XREF---", CARD_UNKNOWN),
                dailyTranLine("TX-003-NO-ACCT----", CARD_ORPHAN_XREF));

        JobExecution execution = StepAssertions.runJob(jobLauncherTestUtils, file);
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = StepAssertions.validationStep(execution);
        assertThat(step.getReadCount()).isEqualTo(3);
        assertThat(step.getWriteCount()).isEqualTo(1);
        assertThat(step.getFilterCount()).isEqualTo(2);
    }

    /**
     * Nested helpers so Spring Batch test listeners do not treat methods on the test class as
     * {@code @JobScope} / {@code @StepScope} factories.
     */
    private static final class StepAssertions {
        static JobExecution runJob(JobLauncherTestUtils utils, Path inputFile) throws Exception {
            return utils.launchJob(new JobParametersBuilder()
                    .addString(
                            DailyTransactionValidationJobConfig.JOB_PARAMETER_INPUT_FILE,
                            inputFile.toAbsolutePath().toString())
                    .addLong("run.id", System.nanoTime())
                    .toJobParameters());
        }

        static StepExecution validationStep(JobExecution execution) {
            return execution.getStepExecutions().stream()
                    .filter(s -> "dailyTransactionValidationStep".equals(s.getStepName()))
                    .findFirst()
                    .orElseThrow();
        }

        private StepAssertions() {}
    }

    private void seedAccount(long acctId) {
        Account a = AccountTestFactory.newAccount();
        a.setAcctId(acctId);
        a.setActiveStatus("Y");
        accountRepository.save(a);
    }

    private static Path writeTempDailyTranFile(String... lines) throws Exception {
        Path p = Files.createTempFile("dalytran-validation", ".dat");
        p.toFile().deleteOnExit();
        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                w.write(DailyTransaction.padToRecordLength(line));
                w.newLine();
            }
        }
        return p;
    }

    /**
     * Minimal CVTRA06Y layout: transaction id (16) and card number (16) at offset 262; remainder
     * space-filled to {@link DailyTransaction#RECORD_LENGTH}.
     */
    private static String dailyTranLine(String transactionId, String cardNumber) {
        char[] buf = new char[DailyTransaction.RECORD_LENGTH];
        Arrays.fill(buf, ' ');
        putField(buf, 0, 16, transactionId);
        putField(buf, 262, 16, cardNumber);
        return new String(buf);
    }

    private static void putField(char[] buf, int start, int length, String value) {
        if (value == null) {
            return;
        }
        int n = Math.min(length, value.length());
        for (int i = 0; i < n; i++) {
            buf[start + i] = value.charAt(i);
        }
    }
}

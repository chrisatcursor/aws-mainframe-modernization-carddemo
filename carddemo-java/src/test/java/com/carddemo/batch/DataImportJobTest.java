package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.account.AccountRepository;
import com.carddemo.account.CustomerRepository;
import com.carddemo.card.CardRepository;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
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
class DataImportJobTest {

    private static final long TEST_CUST_ID = 99001001L;
    private static final long TEST_ACCT_ID = 99001002001L;
    private static final String TEST_CARD_NUM = "9999111111111111";
    private static final String TEST_TRAN_ID = "TESTIMP00000001";

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier(DataImportJobConfig.JOB_NAME)
    private Job dataImportJob;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @AfterEach
    void tearDown() {
        transactionRepository.deleteById(TEST_TRAN_ID);
        cardXrefRepository.findById(TEST_CARD_NUM).ifPresent(cardXrefRepository::delete);
        cardRepository.findById(TEST_CARD_NUM).ifPresent(cardRepository::delete);
        accountRepository.findById(TEST_ACCT_ID).ifPresent(accountRepository::delete);
        customerRepository.findById(TEST_CUST_ID).ifPresent(customerRepository::delete);
    }

    @Test
    void dataImportJob_importsMixedTypes_countsSkips_andSkipsUnknown() throws Exception {
        String ts = "2026-03-30 12:00:00.000000";
        String branch = "0001";
        String region = "NORTH";

        String cLine =
                ImportRecordMapper.formatExportLine(
                        ImportRecordMapper.TYPE_CUSTOMER,
                        ts,
                        1,
                        branch,
                        region,
                        ImportRecordMapper.buildCustomerDataArea(
                                TEST_CUST_ID,
                                "Imp",
                                "Q",
                                "Customer",
                                "1 Main St",
                                "",
                                "",
                                "TX",
                                "USA",
                                "78701",
                                "512-555-0100",
                                "",
                                123456789L,
                                "DL-TX-1",
                                "1990-01-01",
                                "EFT0000001",
                                "Y",
                                720));

        String aLine =
                ImportRecordMapper.formatExportLine(
                        ImportRecordMapper.TYPE_ACCOUNT,
                        ts,
                        2,
                        branch,
                        region,
                        ImportRecordMapper.buildAccountDataArea(
                                TEST_ACCT_ID,
                                "Y",
                                new BigDecimal("100.50"),
                                new BigDecimal("5000.00"),
                                new BigDecimal("500.00"),
                                "2020-01-01",
                                "2028-12-31",
                                "2024-01-01",
                                new BigDecimal("10.00"),
                                new BigDecimal("5.00"),
                                "78701",
                                "GRP001"));

        String xLine =
                ImportRecordMapper.formatExportLine(
                        ImportRecordMapper.TYPE_XREF,
                        ts,
                        3,
                        branch,
                        region,
                        ImportRecordMapper.buildXrefDataArea(TEST_CARD_NUM, TEST_CUST_ID, TEST_ACCT_ID));

        String tLine =
                ImportRecordMapper.formatExportLine(
                        ImportRecordMapper.TYPE_TRANSACTION,
                        ts,
                        4,
                        branch,
                        region,
                        ImportRecordMapper.buildTransactionDataArea(
                                TEST_TRAN_ID,
                                "01",
                                1,
                                "POS TERM",
                                "Import test purchase",
                                new BigDecimal("42.00"),
                                90001L,
                                "Test Mart",
                                "Austin",
                                "78701",
                                TEST_CARD_NUM,
                                "2026-03-30 10:00:00.000000",
                                "2026-03-30 11:00:00.000000"));

        String dLine =
                ImportRecordMapper.formatExportLine(
                        ImportRecordMapper.TYPE_CARD,
                        ts,
                        5,
                        branch,
                        region,
                        ImportRecordMapper.buildCardDataArea(
                                TEST_CARD_NUM, TEST_ACCT_ID, 321, "IMPORT TESTER", "2028-12-31", "Y"));

        String unknownLine =
                ImportRecordMapper.formatExportLine(
                        'Z',
                        ts,
                        6,
                        branch,
                        region,
                        "");

        String goodCustPayload =
                ImportRecordMapper.buildCustomerDataArea(
                        TEST_CUST_ID,
                        "Bad",
                        "",
                        "Parse",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        111223333L,
                        "",
                        "1991-02-02",
                        "",
                        "Y",
                        650);
        String corruptCustPayload = "aaaaaaaaaa" + goodCustPayload.substring(10);
        String badCustomerLine =
                ImportRecordMapper.formatExportLine(
                        ImportRecordMapper.TYPE_CUSTOMER,
                        ts,
                        7,
                        branch,
                        region,
                        corruptCustPayload);

        Path input = Files.createTempFile("cbimport-", ".exp");
        input.toFile().deleteOnExit();
        String content =
                String.join(
                        "\n",
                        cLine,
                        aLine,
                        xLine,
                        tLine,
                        dLine,
                        unknownLine,
                        badCustomerLine);
        Files.writeString(input, content, StandardCharsets.UTF_8);

        jobLauncherTestUtils.setJob(dataImportJob);
        JobExecution execution =
                jobLauncherTestUtils.launchJob(
                        new JobParametersBuilder()
                                .addString("inputFile", input.toAbsolutePath().toString())
                                .addLong("run.id", System.currentTimeMillis())
                                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        StepExecution step =
                execution.getStepExecutions().stream()
                        .filter(s -> DataImportJobConfig.STEP_NAME.equals(s.getStepName()))
                        .findFirst()
                        .orElseThrow();

        assertThat(step.getExecutionContext().getLong(DataImportJobConfig.CTX_COUNT_CUSTOMERS))
                .isEqualTo(1);
        assertThat(step.getExecutionContext().getLong(DataImportJobConfig.CTX_COUNT_ACCOUNTS))
                .isEqualTo(1);
        assertThat(step.getExecutionContext().getLong(DataImportJobConfig.CTX_COUNT_XREFS))
                .isEqualTo(1);
        assertThat(step.getExecutionContext().getLong(DataImportJobConfig.CTX_COUNT_TRANSACTIONS))
                .isEqualTo(1);
        assertThat(step.getExecutionContext().getLong(DataImportJobConfig.CTX_COUNT_CARDS))
                .isEqualTo(1);
        assertThat(step.getExecutionContext().getLong(DataImportJobConfig.CTX_COUNT_INVALID))
                .isEqualTo(2);

        assertThat(customerRepository.findById(TEST_CUST_ID)).isPresent();
        assertThat(customerRepository.findById(TEST_CUST_ID).orElseThrow().getLastName())
                .isEqualTo("Customer");

        assertThat(accountRepository.findById(TEST_ACCT_ID)).isPresent();
        assertThat(accountRepository.findById(TEST_ACCT_ID).orElseThrow().getCurrentBalance())
                .isEqualByComparingTo(new BigDecimal("100.50"));

        assertThat(cardXrefRepository.findById(TEST_CARD_NUM)).isPresent();
        assertThat(cardRepository.findById(TEST_CARD_NUM)).isPresent();
        assertThat(cardRepository.findById(TEST_CARD_NUM).orElseThrow().getCvvCode()).isEqualTo(321);

        assertThat(transactionRepository.findById(TEST_TRAN_ID)).isPresent();
        assertThat(transactionRepository.findById(TEST_TRAN_ID).orElseThrow().getAmount())
                .isEqualByComparingTo(new BigDecimal("42.00"));
    }

    @Test
    void importRecordMapper_detectsRecordTypes() {
        String line =
                ImportRecordMapper.formatExportLine(
                        ImportRecordMapper.TYPE_ACCOUNT,
                        "2026-01-01 00:00:00.000000",
                        1,
                        "0001",
                        "NORTH",
                        ImportRecordMapper.buildAccountDataArea(
                                1L,
                                "Y",
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                "",
                                "",
                                "",
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                "",
                                ""));
        assertThat(new ImportRecordMapper().getRecordType(line)).isEqualTo("A");
    }
}

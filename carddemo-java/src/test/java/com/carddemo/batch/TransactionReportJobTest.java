package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionCategory;
import com.carddemo.transaction.TransactionCategory.TransactionCategoryKey;
import com.carddemo.transaction.TransactionCategoryRepository;
import com.carddemo.transaction.TransactionFactory;
import com.carddemo.transaction.TransactionRepository;
import com.carddemo.transaction.TransactionType;
import com.carddemo.transaction.TransactionTypeRepository;
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
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class TransactionReportJobTest {

    private static final String TEST_CARD_NUM = "1111111111111111";

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("transactionReportJob")
    private Job transactionReportJob;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private TransactionTypeRepository transactionTypeRepository;

    @Autowired
    private TransactionCategoryRepository transactionCategoryRepository;

    @AfterEach
    void tearDown() {
        transactionRepository.deleteById("TSTRPT0000000001");
        for (int i = 0; i < 8; i++) {
            transactionRepository.deleteById(String.format("TSTRPTPG%08d", i));
        }
        cardXrefRepository.findById(TEST_CARD_NUM).ifPresent(cardXrefRepository::delete);
        transactionCategoryRepository
                .findById(new TransactionCategoryKey("01", 1))
                .ifPresent(transactionCategoryRepository::delete);
        transactionTypeRepository.findById("01").ifPresent(transactionTypeRepository::delete);
    }

    @Test
    void transactionReportJob_completesSuccessfully_withEmptyResult() throws Exception {
        Path out = Files.createTempFile("tranrept-empty-", ".txt");
        out.toFile().deleteOnExit();

        jobLauncherTestUtils.setJob(transactionReportJob);
        JobExecution execution =
                jobLauncherTestUtils.launchJob(baseParams(out).addString("startDate", "2099-01-01").addString("endDate", "2099-01-31").toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        String text = Files.readString(out, StandardCharsets.UTF_8);
        assertThat(text).contains("Daily Transaction Report");
        assertThat(text).contains("Date Range:");
        assertThat(text).contains("2099-01-01");
        assertThat(text).contains("2099-01-31");
        assertThat(text).contains("Transaction ID");
        assertThat(text).doesNotContain("Page Total");
        assertThat(text).doesNotContain("Grand Total");
    }

    @Test
    void transactionReportJob_enrichesLines_withTypeAndCategoryDescriptions() throws Exception {
        seedReportReferenceData();

        Transaction tx = TransactionFactory.newTransaction();
        tx.setTransactionId("TSTRPT0000000001");
        tx.setCardNumber(TEST_CARD_NUM);
        tx.setTypeCode("01");
        tx.setCategoryCode(1);
        tx.setSource("POS TERM");
        tx.setDescription("Test purchase");
        tx.setAmount(new BigDecimal("123.45"));
        tx.setProcessedTimestamp("2026-03-15 10:00:00.000000");
        transactionRepository.save(tx);

        Path out = Files.createTempFile("tranrept-enriched-", ".txt");
        out.toFile().deleteOnExit();

        jobLauncherTestUtils.setJob(transactionReportJob);
        JobExecution execution =
                jobLauncherTestUtils.launchJob(baseParams(out).addString("startDate", "2026-03-01").addString("endDate", "2026-03-31").toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        String text = Files.readString(out, StandardCharsets.UTF_8);
        assertThat(text).contains("Purchase");
        assertThat(text).contains("Regular Sales Draft");
        assertThat(text).contains("POS TERM");
        assertThat(text).contains("123.45");
        assertThat(text).contains("Page Total");
        assertThat(text).contains("Grand Total");
    }

    @Test
    void transactionReportJob_paginates_whenLinesPerPageExceeded() throws Exception {
        seedReportReferenceData();

        for (int i = 0; i < 7; i++) {
            Transaction tx = TransactionFactory.newTransaction();
            tx.setTransactionId(String.format("TSTRPTPG%08d", i));
            tx.setCardNumber(TEST_CARD_NUM);
            tx.setTypeCode("01");
            tx.setCategoryCode(1);
            tx.setSource("SRC");
            tx.setAmount(new BigDecimal(i + 1));
            tx.setProcessedTimestamp("2026-05-01 12:00:00.000000");
            transactionRepository.save(tx);
        }

        Path out = Files.createTempFile("tranrept-page-", ".txt");
        out.toFile().deleteOnExit();

        jobLauncherTestUtils.setJob(transactionReportJob);
        JobExecution execution = jobLauncherTestUtils.launchJob(
                baseParams(out)
                        .addString("startDate", "2026-05-01")
                        .addString("endDate", "2026-05-31")
                        .addString("linesPerPage", "3")
                        .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        String text = Files.readString(out, StandardCharsets.UTF_8);
        assertThat(text).contains("Page: 1");
        assertThat(text).contains("Page: 2");
        assertThat(text).contains("Page: 3");
        int pageTotals = text.split("Page Total", -1).length - 1;
        assertThat(pageTotals).isEqualTo(3);
    }

    private void seedReportReferenceData() {
        transactionTypeRepository.save(TransactionType.of("01", "Purchase"));
        transactionCategoryRepository.save(TransactionCategory.of("01", 1, "Regular Sales Draft"));
        cardXrefRepository.save(CardXref.of(TEST_CARD_NUM, 1L, 1L));
    }

    private static JobParametersBuilder baseParams(Path outputFile) {
        return new JobParametersBuilder()
                .addString("outputFile", outputFile.toAbsolutePath().toString())
                .addString("linesPerPage", "60")
                .addLong("run.id", System.currentTimeMillis());
    }
}

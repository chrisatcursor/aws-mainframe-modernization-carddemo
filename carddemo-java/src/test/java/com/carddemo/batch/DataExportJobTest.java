package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.account.Customer;
import com.carddemo.account.CustomerRepository;
import com.carddemo.account.CustomerRepository;
import com.carddemo.account.CustomerTestFactory;
import com.carddemo.card.Card;
import com.carddemo.card.CardRepository;
import com.carddemo.card.CardTestFixtures;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionRepository;
import com.carddemo.transaction.TransactionTestFactory;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
class DataExportJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier(DataExportJobConfig.JOB_NAME)
    private Job dataExportJob;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CardRepository cardRepository;

    private Path outputFile;

    @BeforeEach
    void setUp() throws Exception {
        transactionRepository.deleteAll();
        cardRepository.deleteAll();
        cardXrefRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();

        outputFile = Files.createTempFile("carddemo-export-", ".dat");
        outputFile.toFile().deleteOnExit();

        jobLauncherTestUtils.setJob(dataExportJob);
    }

    @Test
    void dataExportJob_writesHeaderTrailerOnly_whenDatabaseEmpty() throws Exception {
        JobExecution execution = jobLauncherTestUtils.launchJob(jobParams().toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        List<String> lines = Files.readAllLines(outputFile);
        assertThat(lines).hasSize(2);
        assertThat(lines.getFirst().charAt(0)).isEqualTo('H');
        assertThat(lines.getFirst().substring(1, 11)).matches("\\d{4}-\\d{2}-\\d{2}");
        assertThat(lines.getLast().charAt(0)).isEqualTo('Z');
        assertFooterCounts(lines.getLast(), 0, 0, 0, 0, 0, 0);
    }

    @Test
    void dataExportJob_exportsAllEntityTypes_withCorrectPrefixesAndTrailerCounts() throws Exception {
        seedOneOfEach();

        JobExecution execution = jobLauncherTestUtils.launchJob(jobParams().toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        List<String> lines = Files.readAllLines(outputFile);
        assertThat(lines).hasSize(7);

        assertThat(lines.getFirst().charAt(0)).isEqualTo('H');
        LocalDate.parse(lines.getFirst().substring(1, 11));

        assertThat(lines.get(1).charAt(0)).isEqualTo('C');
        assertThat(lines.get(2).charAt(0)).isEqualTo('A');
        assertThat(lines.get(3).charAt(0)).isEqualTo('X');
        assertThat(lines.get(4).charAt(0)).isEqualTo('T');
        assertThat(lines.get(5).charAt(0)).isEqualTo('D');

        assertThat(lines.get(1)).hasSize(DataExportStepContext.RECORD_LENGTH);
        assertThat(lines.get(5)).hasSize(DataExportStepContext.RECORD_LENGTH);

        assertThat(lines.getLast().charAt(0)).isEqualTo('Z');
        assertFooterCounts(lines.getLast(), 1, 1, 1, 1, 1, 5);

        assertThat(lines.get(1).substring(27, 36)).isEqualTo("000000001");
        assertThat(lines.get(2).substring(27, 36)).isEqualTo("000000002");
        assertThat(lines.get(3).substring(27, 36)).isEqualTo("000000003");
        assertThat(lines.get(4).substring(27, 36)).isEqualTo("000000004");
        assertThat(lines.get(5).substring(27, 36)).isEqualTo("000000005");
    }

    private JobParametersBuilder jobParams() {
        return new JobParametersBuilder()
                .addString(DataExportJobConfig.PARAM_OUTPUT_FILE, outputFile.toAbsolutePath().toString())
                .addLong("run.id", System.nanoTime());
    }

    private void seedOneOfEach() {
        Customer c = CustomerTestFactory.newCustomer();
        c.setCustId(90001L);
        c.setFirstName("Ann");
        c.setLastName("Export");
        c.setSsn(123456789L);
        customerRepository.save(c);

        Account a = new Account();
        a.setAcctId(90000000001L);
        a.setActiveStatus("Y");
        a.setCurrentBalance(new BigDecimal("100.00"));
        a.setCreditLimit(new BigDecimal("1000.00"));
        a.setCashCreditLimit(new BigDecimal("500.00"));
        accountRepository.save(a);

        cardXrefRepository.save(CardXref.of("5555555555555555", 90001L, 90000000001L));

        Transaction t = TransactionTestFactory.minimal("EXP0000000000001", "5555555555555555");
        transactionRepository.save(t);

        cardRepository.save(CardTestFixtures.minimalCard("5555555555555555", 90000000001L));
    }

    private static void assertFooterCounts(String footer, long c, long a, long x, long t, long d, long total) {
        assertThat(footer).hasSize(DataExportStepContext.RECORD_LENGTH);
        assertThat(Long.parseLong(footer.substring(1, 10))).isEqualTo(c);
        assertThat(Long.parseLong(footer.substring(10, 19))).isEqualTo(a);
        assertThat(Long.parseLong(footer.substring(19, 28))).isEqualTo(x);
        assertThat(Long.parseLong(footer.substring(28, 37))).isEqualTo(t);
        assertThat(Long.parseLong(footer.substring(37, 46))).isEqualTo(d);
        assertThat(Long.parseLong(footer.substring(46, 55))).isEqualTo(total);
    }
}

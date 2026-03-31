package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StatementGenerationServiceTest {

    private static final String CARD_WITH_TX = "1111111111111111";
    private static final String CARD_NO_TX = "2222222222222222";
    private static final long ACCT_WITH_TX = 101L;
    private static final long ACCT_NO_TX = 102L;
    private static final long CUST_ID = 501L;

    @Autowired
    private StatementGenerationService statementGenerationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Path outputDir;

    @BeforeEach
    void setUp() throws Exception {
        outputDir = Files.createTempDirectory("stmt-gen-test-");

        jdbcTemplate.update(
                "INSERT INTO accounts (acct_id, active_status, curr_bal, version) VALUES (?, ?, ?, ?)",
                ACCT_WITH_TX,
                "Y",
                new BigDecimal("150.25"),
                0L);
        jdbcTemplate.update(
                "INSERT INTO accounts (acct_id, active_status, curr_bal, version) VALUES (?, ?, ?, ?)",
                ACCT_NO_TX,
                "Y",
                new BigDecimal("0.00"),
                0L);

        jdbcTemplate.update(
                "INSERT INTO customers (cust_id, first_name, middle_name, last_name, version) VALUES (?, ?, ?, ?, ?)",
                CUST_ID,
                "Ada",
                "M",
                "Lovelace",
                0L);

        jdbcTemplate.update(
                "INSERT INTO card_xrefs (card_num, cust_id, acct_id) VALUES (?, ?, ?)",
                CARD_WITH_TX,
                CUST_ID,
                ACCT_WITH_TX);
        jdbcTemplate.update(
                "INSERT INTO card_xrefs (card_num, cust_id, acct_id) VALUES (?, ?, ?)",
                CARD_NO_TX,
                CUST_ID,
                ACCT_NO_TX);

        jdbcTemplate.update(
                "INSERT INTO transactions (tran_id, type_cd, description, amount, card_num, proc_ts) VALUES (?, ?, ?, ?, ?, ?)",
                "TXSTMTGEN0000001",
                "01",
                "Coffee Shop",
                new BigDecimal("12.34"),
                CARD_WITH_TX,
                "2026-03-15 10:00:00.000000");
    }

    @Test
    void generatesTextAndHtmlFiles() throws Exception {
        statementGenerationService.generateStatements(outputDir);

        assertThat(outputDir.resolve("statements.txt")).exists();
        assertThat(outputDir.resolve("statements.html")).exists();
    }

    @Test
    void includesCustomerInfoAndTransactionDetails() throws Exception {
        statementGenerationService.generateStatements(outputDir);

        String text = Files.readString(outputDir.resolve("statements.txt"));
        assertThat(text).contains("Ada");
        assertThat(text).contains("Lovelace");
        assertThat(text).contains(String.valueOf(ACCT_WITH_TX));
        assertThat(text).contains(CARD_WITH_TX);
        assertThat(text).contains("Coffee Shop");
        assertThat(text).contains("12.34");
        assertThat(text).contains("2026-03-15");

        String html = Files.readString(outputDir.resolve("statements.html"));
        assertThat(html).contains("Ada");
        assertThat(html).contains("Coffee Shop");
        assertThat(html).contains("12.34");
        assertThat(html).contains("<table");
    }

    @Test
    void handlesAccountWithNoTransactions() throws Exception {
        statementGenerationService.generateStatements(outputDir);

        String text = Files.readString(outputDir.resolve("statements.txt"));
        int idx = text.indexOf(CARD_NO_TX);
        assertThat(idx).isPositive();
        String section = text.substring(idx, Math.min(text.length(), idx + 800));
        assertThat(section).contains("Transaction total:");
        assertThat(section).contains("0.00");
        assertThat(section).contains("Account balance:");

        String html = Files.readString(outputDir.resolve("statements.html"));
        assertThat(html).contains(CARD_NO_TX);
        assertThat(html).contains("Transaction total:");
        assertThat(html).contains("0.00");
    }
}

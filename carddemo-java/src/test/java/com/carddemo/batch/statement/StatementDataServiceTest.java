package com.carddemo.batch.statement;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.account.Account;
import com.carddemo.account.Customer;
import com.carddemo.card.CardXref;
import com.carddemo.transaction.Transaction;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StatementDataServiceTest {

    private static final String SEED_CARD_NUM = "9680294154603697";
    private static final long SEED_ACCT_ID = 1L;
    private static final long SEED_CUST_ID = 1L;

    @Autowired
    private StatementDataService statementDataService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedStatementFixture() {
        jdbcTemplate.update(
                "INSERT INTO accounts (acct_id, active_status, version) VALUES (?, ?, ?)",
                SEED_ACCT_ID,
                "Y",
                0L);
        jdbcTemplate.update(
                "INSERT INTO customers (cust_id, first_name, version) VALUES (?, ?, ?)",
                SEED_CUST_ID,
                "Immanuel",
                0L);
        jdbcTemplate.update(
                "INSERT INTO card_xrefs (card_num, cust_id, acct_id) VALUES (?, ?, ?)",
                SEED_CARD_NUM,
                SEED_CUST_ID,
                SEED_ACCT_ID);
        jdbcTemplate.update(
                "INSERT INTO transactions (tran_id, type_cd, card_num) VALUES (?, ?, ?)",
                "TXSTMT0000000001",
                "01",
                SEED_CARD_NUM);
    }

    @Test
    void findXrefByCardNumber_returnsData() {
        Optional<CardXref> xref = statementDataService.findXrefByCardNumber(SEED_CARD_NUM);
        assertThat(xref).isPresent();
        assertThat(xref.get().getAccountId()).isEqualTo(SEED_ACCT_ID);
        assertThat(xref.get().getCustomerId()).isEqualTo(SEED_CUST_ID);
    }

    @Test
    void findCustomerById_returnsData() {
        Optional<Customer> customer = statementDataService.findCustomerById(SEED_CUST_ID);
        assertThat(customer).isPresent();
        assertThat(customer.get().getFirstName()).isEqualTo("Immanuel");
    }

    @Test
    void findAccountById_returnsData() {
        Optional<Account> account = statementDataService.findAccountById(SEED_ACCT_ID);
        assertThat(account).isPresent();
        assertThat(account.get().getAcctId()).isEqualTo(SEED_ACCT_ID);
        assertThat(account.get().getActiveStatus()).isEqualTo("Y");
    }

    @Test
    void getStatementDataForAccount_assemblesAllPieces() {
        StatementData data = statementDataService.getStatementDataForAccount(SEED_ACCT_ID);
        assertThat(data.account()).isNotNull();
        assertThat(data.account().getAcctId()).isEqualTo(SEED_ACCT_ID);
        assertThat(data.customer()).isNotNull();
        assertThat(data.customer().getCustId()).isEqualTo(SEED_CUST_ID);
        assertThat(data.cards()).isNotEmpty();
        assertThat(data.cards()).anyMatch(x -> SEED_CARD_NUM.equals(x.getCardNumber()));
        assertThat(data.transactions()).isNotEmpty();
        assertThat(data.transactions()).anyMatch(t -> SEED_CARD_NUM.equals(t.getCardNumber()));
    }

    @Test
    void findXrefByCardNumber_missing_returnsEmpty() {
        assertThat(statementDataService.findXrefByCardNumber("0000000000000000")).isEmpty();
    }

    @Test
    void findCustomerById_missing_returnsEmpty() {
        assertThat(statementDataService.findCustomerById(999_999L)).isEmpty();
    }

    @Test
    void findAccountById_missing_returnsEmpty() {
        assertThat(statementDataService.findAccountById(999_999L)).isEmpty();
    }

    @Test
    void findTransactionsByCardNumber_missing_returnsEmptyList() {
        Pageable pageable = PageRequest.of(0, 50);
        List<Transaction> list =
                statementDataService.findTransactionsByCardNumber("0000000000000000", pageable);
        assertThat(list).isEmpty();
    }

    @Test
    void getStatementDataForAccount_missingAccount_returnsEmptyShell() {
        StatementData data = statementDataService.getStatementDataForAccount(999_999L);
        assertThat(data.account()).isNull();
        assertThat(data.customer()).isNull();
        assertThat(data.cards()).isEmpty();
        assertThat(data.transactions()).isEmpty();
    }
}

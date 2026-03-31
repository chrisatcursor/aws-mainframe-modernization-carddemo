package com.carddemo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.account.AccountTestFactory;
import com.carddemo.batch.InterestCalculationService;
import com.carddemo.batch.TransactionPostingProcessor;
import com.carddemo.batch.DailyTransactionRecord;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.DisclosureGroupRepository;
import com.carddemo.transaction.DisclosureGroupTestFactory;
import com.carddemo.transaction.PaymentResult;
import com.carddemo.transaction.PaymentService;
import com.carddemo.transaction.TransactionCategoryBalance;
import com.carddemo.transaction.TransactionCategoryBalance.TransactionCategoryBalanceKey;
import com.carddemo.transaction.TransactionCategoryBalanceRepository;
import com.carddemo.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/integration/create-transaction-id-seq.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class FinancialPrecisionTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InterestCalculationService interestCalculationService;

    @Autowired
    private TransactionPostingProcessor transactionPostingProcessor;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionCategoryBalanceRepository transactionCategoryBalanceRepository;

    @Autowired
    private DisclosureGroupRepository disclosureGroupRepository;

    @BeforeEach
    void wipeFinancialTables() {
        jdbcTemplate.update("delete from transactions");
        jdbcTemplate.update("delete from transaction_category_balances");
        jdbcTemplate.update("delete from disclosure_groups");
        jdbcTemplate.update("delete from card_xrefs");
        jdbcTemplate.update("delete from cards");
        jdbcTemplate.update("delete from accounts");
        jdbcTemplate.update("delete from customers");
    }

    @Test
    void interestCalculation_knownRateAndBalance_matchesHalfUpMonthlyFormula() {
        Long acctId = 8800101L;
        persistInterestFixture(acctId, "GRPC1", "01", 3, new BigDecimal("12.00"), new BigDecimal("1200.00"));

        interestCalculationService.calculateInterest();

        BigDecimal expectedMonthly =
                new BigDecimal("1200.00")
                        .multiply(new BigDecimal("12.00"))
                        .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                        .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);

        assertThat(expectedMonthly.compareTo(new BigDecimal("12.00"))).isZero();

        Account reloaded = accountRepository.findById(acctId).orElseThrow();
        assertThat(reloaded.getCurrentBalance().compareTo(expectedMonthly)).isZero();

        assertThat(
                        transactionRepository.findAll().stream()
                                .filter(t -> "05".equals(t.getTypeCode()))
                                .findFirst()
                                .orElseThrow()
                                .getAmount()
                                .compareTo(expectedMonthly))
                .isZero();
    }

    @Test
    void transactionPosting_balanceUpdate_matchesBigDecimalArithmetic() {
        Long acctId = 8800102L;
        String card = "5999999999999999";
        BigDecimal start = new BigDecimal("100.00");
        BigDecimal movement = new BigDecimal("37.49");

        Account account = AccountTestFactory.newAccount();
        account.setAcctId(acctId);
        account.setActiveStatus("Y");
        account.setCurrentBalance(start);
        account.setCreditLimit(new BigDecimal("1000.00"));
        account.setExpirationDate("2032-01-01");
        account.setCurrentCycleDebit(BigDecimal.ZERO);
        account.setCurrentCycleCredit(BigDecimal.ZERO);
        accountRepository.save(account);
        cardXrefRepository.save(CardXref.of(card, 99001L, acctId));

        BigDecimal expected =
                start.add(movement).setScale(2, RoundingMode.UNNECESSARY);

        DailyTransactionRecord row =
                new DailyTransactionRecord(
                        "0000000000000999",
                        card,
                        "01",
                        4,
                        movement,
                        1L,
                        "Store",
                        "City",
                        "00000",
                        "2026-01-01 12:00:00.000000");

        transactionPostingProcessor.process(row);

        Account after = accountRepository.findById(acctId).orElseThrow();
        assertThat(after.getCurrentBalance().compareTo(expected)).isZero();
    }

    @Test
    void billPayment_fullBalanceDeductionIsExact() {
        Long acctId = 8800103L;
        BigDecimal balance = new BigDecimal("8844.07");

        Account account = AccountTestFactory.newAccount();
        account.setAcctId(acctId);
        account.setActiveStatus("Y");
        account.setCurrentBalance(balance);
        account.setCreditLimit(new BigDecimal("20000.00"));
        account.setExpirationDate("2032-01-01");
        accountRepository.save(account);
        cardXrefRepository.save(CardXref.of("6000000000000006", 99002L, acctId));

        PaymentResult result = paymentService.payBill(acctId);

        assertThat(result.success()).isTrue();
        assertThat(result.paidAmount().compareTo(balance)).isZero();

        Account after = accountRepository.findById(acctId).orElseThrow();
        assertThat(after.getCurrentBalance().compareTo(BigDecimal.ZERO)).isZero();
    }

    private void persistInterestFixture(
            Long acctId,
            String groupId,
            String typeCode,
            int categoryCode,
            BigDecimal annualRatePercent,
            BigDecimal categoryBalance) {
        Account account = AccountTestFactory.newAccount();
        account.setAcctId(acctId);
        account.setActiveStatus("Y");
        account.setGroupId(groupId);
        account.setCurrentBalance(BigDecimal.ZERO);
        account.setCreditLimit(new BigDecimal("5000.00"));
        account.setExpirationDate("2032-01-01");
        accountRepository.save(account);

        disclosureGroupRepository.save(
                DisclosureGroupTestFactory.newDisclosureGroup(
                        groupId, typeCode, categoryCode, annualRatePercent));

        TransactionCategoryBalanceKey key =
                new TransactionCategoryBalanceKey(acctId, typeCode, categoryCode);
        transactionCategoryBalanceRepository.save(
                TransactionCategoryBalance.newWithIdAndBalance(key, categoryBalance));
    }
}

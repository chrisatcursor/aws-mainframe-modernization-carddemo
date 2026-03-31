package com.carddemo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.account.AccountTestFactory;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.PaymentResult;
import com.carddemo.transaction.PaymentService;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import jakarta.persistence.EntityManager;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/integration/create-transaction-id-seq.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Transactional
class PaymentIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Test
    void payBill_atomicity_createsTransactionAndZerosBalanceInSameLogicalUnit() {
        BigDecimal original = new BigDecimal("999999.99");
        Long acctId = 71001L;
        persistBillPayFixture(acctId, "5111111111111111", 91001L, original);

        PaymentResult result = paymentService.payBill(acctId);

        assertThat(result.success()).isTrue();
        assertThat(result.transactionId()).isNotNull();

        Account reloaded = accountRepository.findById(acctId).orElseThrow();
        assertThat(reloaded.getCurrentBalance().compareTo(BigDecimal.ZERO)).isZero();

        Transaction saved =
                transactionRepository.findById(result.transactionId()).orElseThrow();
        assertThat(saved.getTypeCode()).isEqualTo("02");
        assertThat(saved.getCategoryCode()).isEqualTo(2);
        assertThat(saved.getSource()).isEqualTo("BILLPAY");
        assertThat(saved.getAmount().compareTo(original)).isZero();
    }

    @Test
    void payBill_bigDecimalPrecision_paymentAmountMatchesBalanceExactly() {
        BigDecimal awkward = new BigDecimal("0.01").add(new BigDecimal("0.02"));
        Long acctId = 71002L;
        persistBillPayFixture(acctId, "5222222222222222", 91002L, awkward);

        PaymentResult result = paymentService.payBill(acctId);

        assertThat(result.success()).isTrue();
        assertThat(result.paidAmount().compareTo(new BigDecimal("0.03"))).isZero();

        Transaction saved =
                transactionRepository.findById(result.transactionId()).orElseThrow();
        assertThat(saved.getAmount().compareTo(new BigDecimal("0.03"))).isZero();
    }

    private void persistBillPayFixture(Long acctId, String cardNum, Long custId, BigDecimal balance) {
        Account account = AccountTestFactory.newAccount();
        account.setAcctId(acctId);
        account.setActiveStatus("Y");
        account.setCurrentBalance(balance);
        account.setCreditLimit(new BigDecimal("1000000.00"));
        account.setExpirationDate("2035-06-15");
        accountRepository.save(account);

        cardXrefRepository.save(CardXref.of(cardNum, custId, acctId));
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @Sql(scripts = "/integration/create-transaction-id-seq.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
    class WhenTransactionSaveFails {

        @MockBean
        private TransactionRepository transactionRepository;

        @Autowired
        private PaymentService paymentService;

        @Autowired
        private AccountRepository accountRepository;

        @Autowired
        private CardXrefRepository cardXrefRepository;

        @Autowired
        private EntityManager entityManager;

        @Test
        void payBill_whenTransactionSaveFails_accountBalanceUnchanged() {
            Long acctId = 71003L;
            BigDecimal balance = new BigDecimal("123.45");

            Account account = AccountTestFactory.newAccount();
            account.setAcctId(acctId);
            account.setActiveStatus("Y");
            account.setCurrentBalance(balance);
            account.setCreditLimit(new BigDecimal("5000.00"));
            account.setExpirationDate("2035-06-15");
            accountRepository.save(account);
            cardXrefRepository.save(CardXref.of("5333333333333333", 91003L, acctId));

            when(transactionRepository.getNextTransactionId()).thenReturn(200001L);
            doThrow(new RuntimeException("simulated persistence failure"))
                    .when(transactionRepository)
                    .save(any(Transaction.class));

            assertThatThrownBy(() -> paymentService.payBill(acctId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("simulated persistence failure");

            entityManager.clear();

            Account reloaded = accountRepository.findById(acctId).orElseThrow();
            assertThat(reloaded.getCurrentBalance().compareTo(balance)).isZero();
        }
    }
}

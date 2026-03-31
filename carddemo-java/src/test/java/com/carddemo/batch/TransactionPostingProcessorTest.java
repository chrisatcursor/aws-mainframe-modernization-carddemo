package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carddemo.account.Account;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.account.AccountRepository;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionCategoryBalance;
import com.carddemo.transaction.TransactionCategoryBalance.TransactionCategoryBalanceKey;
import com.carddemo.transaction.TransactionCategoryBalanceRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionPostingProcessorTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardXrefRepository cardXrefRepository;

    @Mock
    private TransactionCategoryBalanceRepository transactionCategoryBalanceRepository;

    private TransactionPostingProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TransactionPostingProcessor(
                accountRepository, cardXrefRepository, transactionCategoryBalanceRepository);
    }

    @Test
    void validTransaction_updatesAccountAndCategoryBalance_andReturnsTransaction() {
        String card = "4111111111111111";
        Long acctId = 99L;
        CardXref xref = new CardXref() {};
        xref.setCardNumber(card);
        xref.setAccountId(acctId);

        Account account = new Account() {};
        account.setAcctId(acctId);
        account.setActiveStatus("Y");
        account.setCurrentBalance(new BigDecimal("100.00"));
        account.setCreditLimit(new BigDecimal("500.00"));
        account.setExpirationDate("2030-01-01");
        account.setCurrentCycleDebit(new BigDecimal("10.00"));
        account.setCurrentCycleCredit(new BigDecimal("0.00"));

        TransactionCategoryBalanceKey key = new TransactionCategoryBalanceKey(acctId, "01", 5);

        when(cardXrefRepository.findById(card)).thenReturn(Optional.of(xref));
        when(accountRepository.findById(acctId)).thenReturn(Optional.of(account));
        when(transactionCategoryBalanceRepository.findById(key)).thenReturn(Optional.empty());

        DailyTransactionRecord input = new DailyTransactionRecord(
                "0000000000000001",
                card,
                "01",
                5,
                new BigDecimal("10.00"),
                1L,
                "Coffee Shop",
                "Seattle",
                "98101",
                "2026-01-15 10:00:00.000000");

        Transaction result = processor.process(input);

        assertThat(result).isNotNull();
        assertThat(result.getTransactionId()).isEqualTo("0000000000000001");
        assertThat(result.getAmount()).isEqualByComparingTo("10.00");
        assertThat(result.getCardNumber()).isEqualTo(card);
        assertThat(result.getTypeCode()).isEqualTo("01");
        assertThat(result.getCategoryCode()).isEqualTo(5);
        assertThat(result.getProcessedTimestamp()).isNotBlank();

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getCurrentBalance()).isEqualByComparingTo("110.00");
        assertThat(accountCaptor.getValue().getCurrentCycleDebit()).isEqualByComparingTo("20.00");

        ArgumentCaptor<TransactionCategoryBalance> tcbCaptor = ArgumentCaptor.forClass(TransactionCategoryBalance.class);
        verify(transactionCategoryBalanceRepository).save(tcbCaptor.capture());
        assertThat(tcbCaptor.getValue().getId()).isEqualTo(key);
        assertThat(tcbCaptor.getValue().getBalance()).isEqualByComparingTo("10.00");
    }

    @Test
    void overLimit_returnsNull_andDoesNotPersist() {
        String card = "4222222222222222";
        Long acctId = 2L;
        CardXref xref = new CardXref() {};
        xref.setCardNumber(card);
        xref.setAccountId(acctId);

        Account account = new Account() {};
        account.setAcctId(acctId);
        account.setActiveStatus("Y");
        account.setCurrentBalance(new BigDecimal("100.00"));
        account.setCreditLimit(new BigDecimal("100.00"));
        account.setExpirationDate("2030-01-01");

        when(cardXrefRepository.findById(card)).thenReturn(Optional.of(xref));
        when(accountRepository.findById(acctId)).thenReturn(Optional.of(account));

        DailyTransactionRecord input = new DailyTransactionRecord(
                "0000000000000002", card, "01", 1, new BigDecimal("0.01"), null, null, null, null, null);

        assertThat(processor.process(input)).isNull();
        verify(accountRepository, never()).save(any());
        verify(transactionCategoryBalanceRepository, never()).save(any());
    }

    @Test
    void expiredAccount_returnsNull_andDoesNotPersist() {
        String card = "4333333333333333";
        Long acctId = 3L;
        CardXref xref = new CardXref() {};
        xref.setCardNumber(card);
        xref.setAccountId(acctId);

        Account account = new Account() {};
        account.setAcctId(acctId);
        account.setActiveStatus("Y");
        account.setCurrentBalance(BigDecimal.ZERO);
        account.setCreditLimit(new BigDecimal("1000.00"));
        account.setExpirationDate("2020-01-01");

        when(cardXrefRepository.findById(card)).thenReturn(Optional.of(xref));
        when(accountRepository.findById(acctId)).thenReturn(Optional.of(account));

        DailyTransactionRecord input = new DailyTransactionRecord(
                "0000000000000003", card, "02", 2, new BigDecimal("5.00"), null, null, null, null, null);

        assertThat(processor.process(input)).isNull();
        verify(accountRepository, never()).save(any());
        verify(transactionCategoryBalanceRepository, never()).save(any());
    }

    @Test
    void missingCardXref_returnsNull() {
        String card = "4444444444444444";
        when(cardXrefRepository.findById(card)).thenReturn(Optional.empty());

        DailyTransactionRecord input = new DailyTransactionRecord(
                "0000000000000004", card, "01", 1, BigDecimal.ONE, null, null, null, null, null);

        assertThat(processor.process(input)).isNull();
        verify(accountRepository, never()).save(any());
        verify(transactionCategoryBalanceRepository, never()).save(any());
    }
}

package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.transaction.DisclosureGroup;
import com.carddemo.transaction.DisclosureGroup.DisclosureGroupKey;
import com.carddemo.transaction.DisclosureGroupRepository;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionCategoryBalance;
import com.carddemo.transaction.TransactionCategoryBalance.TransactionCategoryBalanceKey;
import com.carddemo.transaction.TransactionCategoryBalanceRepository;
import com.carddemo.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class InterestCalculationServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionCategoryBalanceRepository transactionCategoryBalanceRepository;

    @Mock
    private DisclosureGroupRepository disclosureGroupRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private InterestCalculationService service;

    @BeforeEach
    void setUp() throws Exception {
        when(transactionManager.getTransaction(any()))
                .thenAnswer(invocation -> new SimpleTransactionStatus());
        doNothing().when(transactionManager).commit(any());
        service = new InterestCalculationService(
                accountRepository,
                transactionCategoryBalanceRepository,
                disclosureGroupRepository,
                transactionRepository,
                transactionManager,
                new BigDecimal("18.99"));
    }

    @Test
    void calculateInterest_usesDisclosureRate_whenGroupMatches() {
        Account account = new Account();
        account.setAcctId(1L);
        account.setGroupId("G1");
        account.setCurrentBalance(new BigDecimal("1000.00"));

        when(accountRepository.findAll()).thenReturn(List.of(account));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        TransactionCategoryBalanceKey key = new TransactionCategoryBalanceKey(1L, "01", 3);
        TransactionCategoryBalance row = TransactionCategoryBalance.newWithIdAndBalance(key, new BigDecimal("1200.00"));
        when(transactionCategoryBalanceRepository.findByIdAcctId(1L)).thenReturn(List.of(row));

        DisclosureGroup dg = mock(DisclosureGroup.class);
        when(dg.getInterestRate()).thenReturn(new BigDecimal("12.00"));
        when(disclosureGroupRepository.findById(new DisclosureGroupKey("G1", "01", 3))).thenReturn(Optional.of(dg));

        service.calculateInterest();

        // 1200 * 12 / 100 / 12 = 12.00
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getCurrentBalance()).isEqualByComparingTo(new BigDecimal("1012.00"));

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction saved = txCaptor.getValue();
        assertThat(saved.getTypeCode()).isEqualTo("05");
        assertThat(saved.getCategoryCode()).isEqualTo(0);
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("12.00"));
        assertThat(saved.getDescription()).isEqualTo("INTEREST");
    }

    @Test
    void calculateInterest_usesDefaultRate_whenDisclosureMissing() {
        Account account = new Account();
        account.setAcctId(2L);
        account.setGroupId("G1");
        account.setCurrentBalance(BigDecimal.ZERO);

        when(accountRepository.findAll()).thenReturn(List.of(account));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(account));

        TransactionCategoryBalanceKey key = new TransactionCategoryBalanceKey(2L, "99", 1);
        TransactionCategoryBalance row = TransactionCategoryBalance.newWithIdAndBalance(key, new BigDecimal("1200.00"));
        when(transactionCategoryBalanceRepository.findByIdAcctId(2L)).thenReturn(List.of(row));
        when(disclosureGroupRepository.findById(new DisclosureGroupKey("G1", "99", 1))).thenReturn(Optional.empty());

        service.calculateInterest();

        // 1200 * 18.99 / 100 / 12 = 18.99
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("18.99"));
    }

    @Test
    void calculateInterest_roundsMonthlyLineToTwoHalfUp() {
        Account account = new Account();
        account.setAcctId(3L);
        account.setGroupId("G1");
        account.setCurrentBalance(BigDecimal.ZERO);

        when(accountRepository.findAll()).thenReturn(List.of(account));
        when(accountRepository.findById(3L)).thenReturn(Optional.of(account));

        TransactionCategoryBalanceKey key = new TransactionCategoryBalanceKey(3L, "01", 0);
        TransactionCategoryBalance row = TransactionCategoryBalance.newWithIdAndBalance(key, new BigDecimal("333.33"));
        when(transactionCategoryBalanceRepository.findByIdAcctId(3L)).thenReturn(List.of(row));

        DisclosureGroup dg = mock(DisclosureGroup.class);
        when(dg.getInterestRate()).thenReturn(new BigDecimal("10.00"));
        when(disclosureGroupRepository.findById(new DisclosureGroupKey("G1", "01", 0))).thenReturn(Optional.of(dg));

        service.calculateInterest();

        // 333.33 * 10 / 100 / 12 = 2.77775 -> 2.78
        BigDecimal expected =
                new BigDecimal("333.33")
                        .multiply(new BigDecimal("10"))
                        .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP)
                        .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);

        assertThat(expected).isEqualByComparingTo(new BigDecimal("2.78"));

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getAmount().scale()).isEqualTo(2);
        assertThat(txCaptor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("2.78"));
    }
}

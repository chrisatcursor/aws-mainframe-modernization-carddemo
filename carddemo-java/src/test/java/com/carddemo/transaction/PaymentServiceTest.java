package com.carddemo.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CardXrefRepository cardXrefRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void successfulPayment_deductsFullBalance_andCreatesType02Category2Transaction() {
        Account account = new Account();
        account.setAcctId(1L);
        account.setCurrentBalance(new BigDecimal("100.50"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(cardXrefRepository.findByAccountId(1L))
                .thenReturn(List.of(CardXref.of("4111111111111111", 9L, 1L)));
        when(transactionRepository.getNextTransactionId()).thenReturn(42L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResult result = paymentService.payBill(1L);

        assertThat(result.success()).isTrue();
        assertThat(result.transactionId()).isEqualTo("0000000000000042");
        assertThat(result.paidAmount().compareTo(new BigDecimal("100.50"))).isZero();
        assertThat(account.getCurrentBalance().compareTo(BigDecimal.ZERO)).isZero();

        verify(transactionRepository)
                .save(
                        argThat(
                                t -> "02".equals(t.getTypeCode())
                                        && t.getCategoryCode() != null
                                        && t.getCategoryCode().compareTo(2) == 0
                                        && "BILLPAY".equals(t.getSource())
                                        && "Bill Payment".equals(t.getDescription())
                                        && t.getAmount().compareTo(new BigDecimal("100.50")) == 0
                                        && "4111111111111111".equals(t.getCardNumber())
                                        && t.getOriginTimestamp() != null
                                        && t.getProcessedTimestamp() != null));
    }

    @Test
    void accountNotFound_throwsEntityNotFoundException() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.payBill(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void zeroBalance_returnsError() {
        Account account = new Account();
        account.setAcctId(2L);
        account.setCurrentBalance(BigDecimal.ZERO);

        when(accountRepository.findById(2L)).thenReturn(Optional.of(account));

        PaymentResult result = paymentService.payBill(2L);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isNotBlank();
        assertThat(result.transactionId()).isNull();
        assertThat(result.paidAmount()).isNull();
    }

    @Test
    void atomicity_savesTransactionAndAccountTogether_onSuccess() {
        Account account = new Account();
        account.setAcctId(3L);
        account.setCurrentBalance(new BigDecimal("10.00"));

        when(accountRepository.findById(3L)).thenReturn(Optional.of(account));
        when(cardXrefRepository.findByAccountId(3L))
                .thenReturn(List.of(CardXref.of("4222222222222222", 1L, 3L)));
        when(transactionRepository.getNextTransactionId()).thenReturn(7L);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.payBill(3L);

        verify(transactionRepository).save(any(Transaction.class));
        verify(accountRepository).save(any(Account.class));
    }
}

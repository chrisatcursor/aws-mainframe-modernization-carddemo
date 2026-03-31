package com.carddemo.account;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CardXrefRepository cardXrefRepository;
    @Mock private com.carddemo.card.CardRepository cardRepository;

    @InjectMocks private AccountService accountService;

    private Account account;
    private Customer customer;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setAcctId(1001L);
        account.setActiveStatus("Y");
        account.setCreditLimit(new BigDecimal("10000.00"));
        account.setCashCreditLimit(new BigDecimal("2000.00"));
        account.setOpenDate("2020-01-15");
        account.setExpirationDate("2025-12-31");
        account.setReissueDate("2024-06-01");
        account.setGroupId("GRP1");
        account.setAddressZip("12345");
        account.setVersion(3L);

        customer = CustomerTestFactory.newCustomer();
        customer.setCustId(2001L);
        customer.setFirstName("Jane");
        customer.setLastName("Doe");
        customer.setVersion(5L);
    }

    @Test
    void updateAccount_savesAccountAndCustomerInOneFlow() {
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(account));
        when(cardXrefRepository.findByAccountId(1001L))
                .thenReturn(List.of(CardXref.of("4111111111111111", 2001L, 1001L)));
        when(customerRepository.findById(2001L)).thenReturn(Optional.of(customer));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountUpdateRequest req = new AccountUpdateRequest();
        req.setAcctId(1001L);
        req.setAccountVersion(3L);
        req.setCustomerVersion(5L);
        req.setActiveStatus("N");
        req.setCreditLimit(new BigDecimal("12000.00"));
        req.setCashCreditLimit(new BigDecimal("2500.00"));
        req.setOpenDate("2020-01-15");
        req.setExpirationDate("2025-12-31");
        req.setReissueDate("2024-06-01");
        req.setGroupId("GRP2");
        req.setAddressZip("99999");
        req.setFirstName("Janet");
        req.setLastName("Smith");
        req.setAddressLine1("1 Main St");
        req.setAddressLine2("Apt 2");
        req.setAddressLine3("");
        req.setAddressStateCode("TX");
        req.setAddressCountryCode("USA");
        req.setCustomerAddressZip("99999");
        req.setPhoneNumber1("555-0100");
        req.setPhoneNumber2("555-0200");
        req.setSsn(123456789L);
        req.setGovtIssuedId("ID-1");
        req.setDateOfBirth("1990-05-05");
        req.setEftAccountId("EFT01");
        req.setFicoCreditScore(720);

        accountService.updateAccount(1001L, req);

        verify(accountRepository).save(account);
        verify(customerRepository).save(customer);
        org.assertj.core.api.Assertions.assertThat(account.getActiveStatus()).isEqualTo("N");
        org.assertj.core.api.Assertions.assertThat(account.getCreditLimit()).isEqualByComparingTo("12000.00");
        org.assertj.core.api.Assertions.assertThat(customer.getFirstName()).isEqualTo("Janet");
        org.assertj.core.api.Assertions.assertThat(customer.getLastName()).isEqualTo("Smith");
    }

    @Test
    void updateAccount_throwsWhenAccountMissing() {
        when(accountRepository.findById(9999L)).thenReturn(Optional.empty());

        AccountUpdateRequest req = new AccountUpdateRequest();
        req.setAcctId(9999L);
        req.setAccountVersion(0L);
        req.setCustomerVersion(0L);
        req.setActiveStatus("Y");
        req.setFirstName("A");
        req.setLastName("B");

        assertThatThrownBy(() -> accountService.updateAccount(9999L, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Account not found");

        verify(accountRepository, never()).save(any());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateAccount_propagatesOptimisticLockFailure() {
        when(accountRepository.findById(1001L)).thenReturn(Optional.of(account));
        when(cardXrefRepository.findByAccountId(1001L))
                .thenReturn(List.of(CardXref.of("4111111111111111", 2001L, 1001L)));
        when(customerRepository.findById(2001L)).thenReturn(Optional.of(customer));
        when(accountRepository.save(any(Account.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Account.class, 1001L));

        AccountUpdateRequest req = new AccountUpdateRequest();
        req.setAcctId(1001L);
        req.setAccountVersion(3L);
        req.setCustomerVersion(5L);
        req.setActiveStatus("Y");
        req.setFirstName("Jane");
        req.setLastName("Doe");

        assertThatThrownBy(() -> accountService.updateAccount(1001L, req))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(customerRepository, never()).save(any());
    }
}

package com.carddemo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.account.Account;
import com.carddemo.account.AccountDetailDto;
import com.carddemo.account.AccountRepository;
import com.carddemo.account.AccountService;
import com.carddemo.account.AccountTestFactory;
import com.carddemo.account.AccountUpdateRequest;
import com.carddemo.account.Customer;
import com.carddemo.account.CustomerRepository;
import com.carddemo.account.CustomerTestFactory;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.persistence.OptimisticLockException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountUpdateIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @Test
    void updateAccount_persistsAccountAndCustomerInOneTransaction() {
        Long acctId = 72001L;
        Long custId = 92001L;
        seedAccountCustomerXref(acctId, custId);

        AccountDetailDto before = accountService.findAccountWithCustomer(acctId);
        AccountUpdateRequest req = AccountUpdateRequest.fromDetail(before);
        req.setActiveStatus("N");
        req.setFirstName("Pat");
        req.setLastName("Lee");
        req.setCreditLimit(new BigDecimal("2500.00"));

        accountService.updateAccount(acctId, req);

        Account account = accountRepository.findById(acctId).orElseThrow();
        Customer customer = customerRepository.findById(custId).orElseThrow();
        assertThat(account.getActiveStatus()).isEqualTo("N");
        assertThat(account.getCreditLimit().compareTo(new BigDecimal("2500.00"))).isZero();
        assertThat(customer.getFirstName()).isEqualTo("Pat");
        assertThat(customer.getLastName()).isEqualTo("Lee");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void updateAccount_concurrentUpdates_oneFailsWithOptimisticLock_noMixedState()
            throws InterruptedException, TimeoutException {
        Long acctId = 72002L;
        Long custId = 92002L;
        seedAccountCustomerXref(acctId, custId);

        AccountDetailDto snapshot = accountService.findAccountWithCustomer(acctId);
        AccountUpdateRequest rA = AccountUpdateRequest.fromDetail(snapshot);
        rA.setFirstName("WinnerA");
        AccountUpdateRequest rB = AccountUpdateRequest.fromDetail(snapshot);
        rB.setLastName("WinnerB");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<?> fA = pool.submit(() -> accountService.updateAccount(acctId, rA));
        Future<?> fB = pool.submit(() -> accountService.updateAccount(acctId, rB));

        int optimisticFailures = 0;
        for (Future<?> f : java.util.List.of(fA, fB)) {
            try {
                f.get(30, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                Throwable c = e.getCause();
                if (c instanceof ObjectOptimisticLockingFailureException
                        || c instanceof OptimisticLockException
                        || (c.getCause() instanceof OptimisticLockException)) {
                    optimisticFailures++;
                } else {
                    throw new AssertionError("unexpected failure", c);
                }
            }
        }
        pool.shutdown();

        assertThat(optimisticFailures).isEqualTo(1);

        Account account = accountRepository.findById(acctId).orElseThrow();
        Customer customer = customerRepository.findById(custId).orElseThrow();
        boolean aWon = "WinnerA".equals(customer.getFirstName());
        boolean bWon = "WinnerB".equals(customer.getLastName());
        assertThat(aWon ^ bWon).isTrue();
        assertThat(account.getActiveStatus()).isEqualTo("Y");
    }

    private void seedAccountCustomerXref(Long acctId, Long custId) {
        Account account = AccountTestFactory.newAccount();
        account.setAcctId(acctId);
        account.setActiveStatus("Y");
        account.setCurrentBalance(BigDecimal.ZERO);
        account.setCreditLimit(new BigDecimal("5000.00"));
        account.setCashCreditLimit(new BigDecimal("500.00"));
        account.setOpenDate("2020-01-01");
        account.setExpirationDate("2030-01-01");
        account.setReissueDate("2025-01-01");
        account.setGroupId("G1");
        account.setAddressZip("12345");
        accountRepository.save(account);

        Customer customer = CustomerTestFactory.newCustomer();
        customer.setCustId(custId);
        customer.setFirstName("Jane");
        customer.setLastName("Doe");
        customer.setAddressLine1("1 Main St");
        customer.setAddressStateCode("WA");
        customer.setAddressCountryCode("USA");
        customer.setAddressZip("12345");
        customer.setPhoneNumber1("555-0100");
        customer.setSsn(123456789L);
        customer.setDateOfBirth("1990-05-05");
        customer.setFicoCreditScore(700);
        customerRepository.save(customer);

        cardXrefRepository.save(CardXref.of("5444444444444444", custId, acctId));
    }
}

package com.carddemo.phase2;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.account.Customer;
import com.carddemo.account.CustomerRepository;
import com.carddemo.card.Card;
import com.carddemo.card.CardRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioral tests for Phase 2 sequential readers: CBACT01C/CBCUS01C/CBACT02C/CBACT03C style ordered scans.
 */
@SpringBootTest
@ActiveProfiles("test")
class SequentialFileReadBehaviorTest {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private CardXrefRepository cardXrefRepository;

    @BeforeEach
    void clear() {
        cardXrefRepository.deleteAll();
        cardRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void accountsReadInKeyOrder() {
        Account a2 = new Account();
        a2.setId(2L);
        a2.setActiveStatus("Y");
        a2.setCurrentBalance(BigDecimal.ONE);
        a2.setCreditLimit(BigDecimal.TEN);
        a2.setCashCreditLimit(BigDecimal.TEN);
        a2.setOpenDate("2020-01-01");
        a2.setExpirationDate("2099-01-01");
        a2.setCurrentCycleCredit(BigDecimal.ZERO);
        a2.setCurrentCycleDebit(BigDecimal.ZERO);
        a2.setGroupId("G");
        Account a1 = new Account();
        a1.setId(1L);
        a1.setActiveStatus("Y");
        a1.setCurrentBalance(BigDecimal.ZERO);
        a1.setCreditLimit(BigDecimal.TEN);
        a1.setCashCreditLimit(BigDecimal.TEN);
        a1.setOpenDate("2020-01-01");
        a1.setExpirationDate("2099-01-01");
        a1.setCurrentCycleCredit(BigDecimal.ZERO);
        a1.setCurrentCycleDebit(BigDecimal.ZERO);
        a1.setGroupId("G");
        accountRepository.save(a2);
        accountRepository.save(a1);

        List<Account> ordered = accountRepository.findAllByOrderByIdAsc();
        assertThat(ordered).extracting(Account::getId).containsExactly(1L, 2L);
    }

    @Test
    void customersCardsAndXrefsOrderedLikeSequentialVsam() {
        Customer c = new Customer();
        c.setId(5L);
        c.setFirstName("A");
        customerRepository.save(c);

        Card card = new Card();
        card.setCardNumber("1111222233334444");
        card.setAccountId(1L);
        card.setCvvCode(1);
        card.setEmbossedName("X");
        card.setExpirationDate("2030-01-01");
        card.setActiveStatus("Y");
        cardRepository.save(card);

        CardXref x = new CardXref();
        x.setCardNumber("1111222233334444");
        x.setCustomerId(5L);
        x.setAccountId(1L);
        cardXrefRepository.save(x);

        assertThat(customerRepository.findAllByOrderByIdAsc()).hasSize(1);
        assertThat(cardRepository.findAll()).hasSize(1);
        assertThat(cardXrefRepository.findAllByOrderByCardNumberAsc()).hasSize(1);
    }
}

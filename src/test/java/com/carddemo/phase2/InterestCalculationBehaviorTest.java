package com.carddemo.phase2;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.batch.InterestCalculationService;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.DisclosureGroup;
import com.carddemo.transaction.DisclosureGroupRepository;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionCategoryBalance;
import com.carddemo.transaction.TransactionCategoryBalanceRepository;
import com.carddemo.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioral tests aligned with CBACT04C interest accrual and account cycle reset.
 */
@SpringBootTest
@ActiveProfiles("test")
class InterestCalculationBehaviorTest {

    @Autowired
    private InterestCalculationService interestCalculationService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CardXrefRepository cardXrefRepository;
    @Autowired
    private TransactionCategoryBalanceRepository tcatRepository;
    @Autowired
    private DisclosureGroupRepository disclosureGroupRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        tcatRepository.deleteAll();
        disclosureGroupRepository.deleteAll();
        cardXrefRepository.deleteAll();
        accountRepository.deleteAll();

        DisclosureGroup dg = new DisclosureGroup();
        DisclosureGroup.DisclosureGroupKey k = new DisclosureGroup.DisclosureGroupKey();
        k.setAccountGroupId("DEFAULT");
        k.setTranTypeCode("01");
        k.setTranCategoryCode(5);
        dg.setId(k);
        dg.setInterestRate(new BigDecimal("12.00"));
        disclosureGroupRepository.save(dg);

        Account a = new Account();
        a.setId(20000000002L);
        a.setActiveStatus("Y");
        a.setCurrentBalance(new BigDecimal("100.00"));
        a.setCreditLimit(new BigDecimal("5000.00"));
        a.setCashCreditLimit(new BigDecimal("1000.00"));
        a.setOpenDate("2020-01-01");
        a.setExpirationDate("2099-12-31");
        a.setCurrentCycleCredit(new BigDecimal("50.00"));
        a.setCurrentCycleDebit(new BigDecimal("25.00"));
        a.setGroupId("DEFAULT");
        accountRepository.save(a);

        CardXref x = new CardXref();
        x.setCardNumber("4333333333333333");
        x.setCustomerId(2L);
        x.setAccountId(20000000002L);
        cardXrefRepository.save(x);

        TransactionCategoryBalance tcb = new TransactionCategoryBalance();
        TransactionCategoryBalance.TransactionCategoryBalanceKey tk =
                new TransactionCategoryBalance.TransactionCategoryBalanceKey();
        tk.setAccountId(20000000002L);
        tk.setTypeCode("01");
        tk.setCategoryCode(5);
        tcb.setId(tk);
        tcb.setBalance(new BigDecimal("600.00"));
        tcatRepository.save(tcb);
    }

    @Test
    void appliesMonthlyInterestAndClearsCycleBalances() {
        interestCalculationService.run("2026-04-01");

        Account a = accountRepository.findById(20000000002L).orElseThrow();
        assertThat(a.getCurrentCycleCredit()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(a.getCurrentCycleDebit()).isEqualByComparingTo(BigDecimal.ZERO);
        // 600 * 12 / 1200 = 6.00 interest
        assertThat(a.getCurrentBalance()).isEqualByComparingTo("106.00");

        List<Transaction> txs = transactionRepository.findAll();
        assertThat(txs).hasSize(1);
        assertThat(txs.getFirst().getAmount()).isEqualByComparingTo("6.00");
        assertThat(txs.getFirst().getTypeCode()).isEqualTo("01");
        assertThat(txs.getFirst().getCategoryCode()).isEqualTo(5);
    }
}

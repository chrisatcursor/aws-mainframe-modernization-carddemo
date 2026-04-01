package com.carddemo.phase2;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.account.Customer;
import com.carddemo.account.CustomerRepository;
import com.carddemo.batch.StatementGenerationService;
import com.carddemo.card.Card;
import com.carddemo.card.CardRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class StatementGenerationBehaviorTest {

    @Autowired
    private StatementGenerationService statementGenerationService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private CardXrefRepository cardXrefRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        cardXrefRepository.deleteAll();
        cardRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();

        Customer c = new Customer();
        c.setId(7L);
        c.setFirstName("Sam");
        c.setLastName("River");
        c.setAddrLine1("Line1");
        c.setAddrLine3("Town");
        c.setAddrStateCd("TX");
        c.setAddrZip("73301");
        c.setAddrCountryCd("USA");
        c.setFicoCreditScore(680);
        customerRepository.save(c);

        Account a = new Account();
        a.setId(40000000004L);
        a.setActiveStatus("Y");
        a.setCurrentBalance(new BigDecimal("42.00"));
        a.setCreditLimit(new BigDecimal("1000.00"));
        a.setCashCreditLimit(new BigDecimal("200.00"));
        a.setOpenDate("2022-01-01");
        a.setExpirationDate("2099-01-01");
        a.setCurrentCycleCredit(BigDecimal.ZERO);
        a.setCurrentCycleDebit(BigDecimal.ZERO);
        a.setGroupId("DEFAULT");
        accountRepository.save(a);

        Card card = new Card();
        card.setCardNumber("5555555555555555");
        card.setAccountId(40000000004L);
        card.setCvvCode(111);
        card.setEmbossedName("SAM RIVER");
        card.setExpirationDate("2031-01-01");
        card.setActiveStatus("Y");
        cardRepository.save(card);

        CardXref x = new CardXref();
        x.setCardNumber("5555555555555555");
        x.setCustomerId(7L);
        x.setAccountId(40000000004L);
        cardXrefRepository.save(x);

        Transaction t = new Transaction();
        t.setId("STMTTXN00000001");
        t.setTypeCode("01");
        t.setCategoryCode(1);
        t.setSource("POS");
        t.setDescription("Coffee");
        t.setAmount(new BigDecimal("3.50"));
        t.setMerchantId(1L);
        t.setMerchantName("Cafe");
        t.setMerchantCity("Austin");
        t.setMerchantZip("78701");
        t.setCardNumber("5555555555555555");
        t.setOrigTimestamp("2026-04-01-10.00.00.000000");
        t.setProcTimestamp("2026-04-01-10.00.01.000000");
        transactionRepository.save(t);
    }

    @Test
    void writesTextAndHtmlStatementFiles() throws Exception {
        Path textDir = Files.createTempDirectory("stmt-txt");
        Path htmlDir = Files.createTempDirectory("stmt-html");
        int n = statementGenerationService.generate(textDir, htmlDir);
        assertThat(n).isEqualTo(1);
        Path txt = textDir.resolve("stmt-acct-40000000004.txt");
        Path html = htmlDir.resolve("stmt-acct-40000000004.html");
        assertThat(Files.exists(txt)).isTrue();
        assertThat(Files.exists(html)).isTrue();
        String content = Files.readString(txt);
        assertThat(content).contains("START OF STATEMENT");
        assertThat(content).contains("Coffee");
        assertThat(content).contains("3.50");
    }
}

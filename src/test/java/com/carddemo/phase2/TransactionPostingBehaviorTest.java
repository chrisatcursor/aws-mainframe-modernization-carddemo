package com.carddemo.phase2;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.batch.DalyTranRecordParser;
import com.carddemo.batch.TransactionPostingService;
import com.carddemo.card.Card;
import com.carddemo.card.CardRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionCategoryBalanceRepository;
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

/**
 * Behavioral tests aligned with CBTRN02C: validation (xref, account, credit limit, expiration) and posting.
 */
@SpringBootTest
@ActiveProfiles("test")
class TransactionPostingBehaviorTest {

    @Autowired
    private TransactionPostingService postingService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private CardXrefRepository cardXrefRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TransactionCategoryBalanceRepository tcatRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        tcatRepository.deleteAll();
        cardXrefRepository.deleteAll();
        cardRepository.deleteAll();
        accountRepository.deleteAll();

        Account a = new Account();
        a.setId(10000000001L);
        a.setActiveStatus("Y");
        a.setCurrentBalance(BigDecimal.ZERO);
        a.setCreditLimit(new BigDecimal("1000.00"));
        a.setCashCreditLimit(new BigDecimal("500.00"));
        a.setOpenDate("2020-01-01");
        a.setExpirationDate("2099-12-31");
        a.setReissueDate("2025-01-01");
        a.setCurrentCycleCredit(BigDecimal.ZERO);
        a.setCurrentCycleDebit(BigDecimal.ZERO);
        a.setGroupId("DEFAULT");
        accountRepository.save(a);

        Card c = new Card();
        c.setCardNumber("5222222222222222");
        c.setAccountId(10000000001L);
        c.setCvvCode(999);
        c.setEmbossedName("TEST USER");
        c.setExpirationDate("2030-01-01");
        c.setActiveStatus("Y");
        cardRepository.save(c);

        CardXref x = new CardXref();
        x.setCardNumber("5222222222222222");
        x.setCustomerId(1L);
        x.setAccountId(10000000001L);
        cardXrefRepository.save(x);
    }

    @Test
    void postsValidTransactionAndUpdatesBalances() throws Exception {
        Path in = Files.createTempFile("daly", ".txt");
        Path rej = Files.createTempFile("rej", ".txt");
        String line = buildLine("T1", "01", 100, "10.00", "5222222222222222", "2099-01-01");
        Files.writeString(in, line + "\n");

        var r = postingService.postFromFile(in, rej);
        assertThat(r.processed()).isEqualTo(1);
        assertThat(r.rejected()).isEqualTo(0);

        Account a = accountRepository.findById(10000000001L).orElseThrow();
        assertThat(a.getCurrentBalance()).isEqualByComparingTo("10.00");
        assertThat(a.getCurrentCycleCredit()).isEqualByComparingTo("10.00");

        Transaction t = transactionRepository.findById("T1").orElseThrow();
        assertThat(t.getAmount()).isEqualByComparingTo("10.00");
        assertThat(t.getCardNumber()).isEqualTo("5222222222222222");
    }

    @Test
    void rejectsUnknownCard() throws Exception {
        Path in = Files.createTempFile("daly", ".txt");
        Path rej = Files.createTempFile("rej", ".txt");
        String line = buildLine("T2", "01", 100, "10.00", "5999999999999999", "2099-01-01");
        Files.writeString(in, line + "\n");

        var r = postingService.postFromFile(in, rej);
        assertThat(r.processed()).isEqualTo(0);
        assertThat(r.rejected()).isEqualTo(1);
    }

    @Test
    void rejectsOverLimit() throws Exception {
        accountRepository.findById(10000000001L).ifPresent(a -> {
            a.setCreditLimit(new BigDecimal("5.00"));
            accountRepository.save(a);
        });
        Path in = Files.createTempFile("daly", ".txt");
        Path rej = Files.createTempFile("rej", ".txt");
        String line = buildLine("T3", "01", 100, "100.00", "5222222222222222", "2099-01-01");
        Files.writeString(in, line + "\n");

        var r = postingService.postFromFile(in, rej);
        assertThat(r.rejected()).isEqualTo(1);
    }

    @Test
    void rejectsAfterAccountExpiration() throws Exception {
        Path in = Files.createTempFile("daly", ".txt");
        Path rej = Files.createTempFile("rej", ".txt");
        String line = buildLine("T4", "01", 100, "1.00", "5222222222222222", "2100-01-01");
        Files.writeString(in, line + "\n");

        var r = postingService.postFromFile(in, rej);
        assertThat(r.rejected()).isEqualTo(1);
    }

    @Test
    void dalyTranParserMatchesFieldLayout() {
        String line = buildLine("ID12345678901234", "02", 5, "-25.50", "5222222222222222", "2024-06-01");
        var p = DalyTranRecordParser.parse(line);
        assertThat(p.id()).isEqualTo("ID12345678901234");
        assertThat(p.typeCode()).isEqualTo("02");
        assertThat(p.categoryCode()).isEqualTo(5);
        assertThat(p.amount()).isEqualByComparingTo("-25.50");
        assertThat(p.cardNumber()).isEqualTo("5222222222222222");
    }

    private static String buildLine(String id, String type, int cat, String amt, String card, String origDatePrefix) {
        StringBuilder sb = new StringBuilder(350);
        sb.append(pad(id, 16));
        sb.append(pad(type, 2));
        sb.append(String.format("%4d", cat).replace(' ', '0'));
        sb.append(pad("SRC", 10));
        sb.append(pad("Test desc", 100));
        sb.append(pad(amt, 11));
        sb.append(String.format("%09d", 0));
        sb.append(pad("Merchant", 50));
        sb.append(pad("City", 50));
        sb.append(pad("ZIP", 10));
        sb.append(pad(card, 16));
        sb.append(pad(origDatePrefix + "-12.34.56.789012", 26));
        sb.append(pad("", 26));
        sb.append(pad("", 20));
        while (sb.length() < 350) {
            sb.append(' ');
        }
        return sb.substring(0, 350);
    }

    private static String pad(String s, int n) {
        if (s == null) {
            s = "";
        }
        if (s.length() > n) {
            return s.substring(0, n);
        }
        return s + " ".repeat(n - s.length());
    }
}

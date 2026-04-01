package com.carddemo.batch;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.account.Customer;
import com.carddemo.account.CustomerRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simplified CBSTM03A: one statement per card xref (account + primary card), plain text + minimal HTML.
 */
@Service
public class StatementGenerationService {

    private final CardXrefRepository cardXrefRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public StatementGenerationService(
            CardXrefRepository cardXrefRepository,
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository) {
        this.cardXrefRepository = cardXrefRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public int generate(Path textDir, Path htmlDir) throws IOException {
        Files.createDirectories(textDir);
        Files.createDirectories(htmlDir);
        List<CardXref> xrefs = cardXrefRepository.findAllByOrderByCardNumberAsc();
        int count = 0;
        for (CardXref xref : xrefs) {
            Optional<Customer> cOpt = customerRepository.findById(xref.getCustomerId());
            Optional<Account> aOpt = accountRepository.findById(xref.getAccountId());
            if (cOpt.isEmpty() || aOpt.isEmpty()) {
                continue;
            }
            Customer c = cOpt.get();
            Account a = aOpt.get();
            List<Transaction> txns = transactionRepository.findByCardNumberOrderByIdAsc(xref.getCardNumber());
            BigDecimal total = txns.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            String baseName = "stmt-acct-" + a.getId();
            String text = buildText(c, a, txns, total);
            Files.writeString(textDir.resolve(baseName + ".txt"), text, StandardCharsets.UTF_8);

            String html = buildHtml(c, a, txns, total);
            Files.writeString(htmlDir.resolve(baseName + ".html"), html, StandardCharsets.UTF_8);
            count++;
        }
        return count;
    }

    private static String buildText(Customer c, Account a, List<Transaction> txns, BigDecimal total) {
        String name = String.join(" ",
                Stream.of(c.getFirstName(), c.getMiddleName(), c.getLastName())
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.toList()));
        StringBuilder sb = new StringBuilder();
        sb.append("*******************************\n");
        sb.append("*******START OF STATEMENT******\n");
        sb.append("*******************************\n");
        sb.append(name).append('\n');
        sb.append(nz(c.getAddrLine1())).append('\n');
        sb.append(nz(c.getAddrLine2())).append('\n');
        sb.append(nz(c.getAddrLine3())).append(' ')
                .append(nz(c.getAddrStateCd())).append(' ')
                .append(nz(c.getAddrCountryCd())).append(' ')
                .append(nz(c.getAddrZip())).append('\n');
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("Account ID         : ").append(a.getId()).append('\n');
        sb.append("Current Balance    : ").append(nz(a.getCurrentBalance())).append('\n');
        sb.append("FICO Score         : ").append(c.getFicoCreditScore() == null ? "" : c.getFicoCreditScore()).append('\n');
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("           TRANSACTION SUMMARY \n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("Tran ID         Tran Details                                    Tran Amount\n");
        sb.append("--------------------------------------------------------------------------------\n");
        for (Transaction t : txns) {
            sb.append(pad(t.getId(), 16)).append(' ')
                    .append(pad(nz(t.getDescription()), 49)).append(" $")
                    .append(nz(t.getAmount())).append('\n');
        }
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("Total EXP:                                              $").append(total).append('\n');
        sb.append("*******************************\n");
        sb.append("*******END OF STATEMENT********\n");
        sb.append("*******************************\n");
        return sb.toString();
    }

    private static String buildHtml(Customer c, Account a, List<Transaction> txns, BigDecimal total) {
        String name = String.join(" ",
                Stream.of(c.getFirstName(), c.getMiddleName(), c.getLastName())
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.toList()));
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>Statement</title></head><body>");
        sb.append("<h3>Statement for Account Number: ").append(a.getId()).append("</h3>");
        sb.append("<p>").append(esc(name)).append("</p>");
        sb.append("<p>").append(esc(nz(c.getAddrLine1()))).append("</p>");
        sb.append("<table border=\"1\"><tr><th>Tran ID</th><th>Details</th><th>Amount</th></tr>");
        for (Transaction t : txns) {
            sb.append("<tr><td>").append(esc(t.getId())).append("</td><td>")
                    .append(esc(nz(t.getDescription()))).append("</td><td>")
                    .append(nz(t.getAmount())).append("</td></tr>");
        }
        sb.append("</table><p><strong>Total: </strong>").append(total).append("</p></body></html>");
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String pad(String s, int n) {
        if (s == null) {
            s = "";
        }
        return s.length() >= n ? s.substring(0, n) : s + " ".repeat(n - s.length());
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static BigDecimal nz(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }
}

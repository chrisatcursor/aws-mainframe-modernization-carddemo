package com.carddemo.batch;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.account.Customer;
import com.carddemo.account.CustomerRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.common.DateTimeHelper;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Statement file generation migrated from COBOL CBSTM03A (XREFFILE drive, STMTFILE / HTMLFILE).
 * CBSTM03B-style I/O is inlined via JPA repositories; ALTER/GO TO becomes sequential steps per card.
 */
@Service
public class StatementGenerationService {

    private static final DecimalFormat MONEY =
            new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));

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

    /**
     * Writes {@code statements.txt} (fixed-width style) and {@code statements.html} under {@code outputDir}.
     */
    public void generateStatements(Path outputDir) throws IOException {
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir is required");
        }
        Files.createDirectories(outputDir);

        List<CardXref> xrefs =
                cardXrefRepository.findAll().stream()
                        .sorted(Comparator.comparing(CardXref::getCardNumber, Comparator.nullsFirst(String::compareTo)))
                        .toList();

        StringBuilder text = new StringBuilder();
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Statements</title></head><body>\n");

        for (CardXref xref : xrefs) {
            appendStatementForXref(xref, text, html);
        }

        html.append("</body></html>\n");

        Path txtPath = outputDir.resolve("statements.txt");
        Path htmlPath = outputDir.resolve("statements.html");
        Files.writeString(txtPath, text.toString(), StandardCharsets.UTF_8);
        Files.writeString(htmlPath, html.toString(), StandardCharsets.UTF_8);
    }

    private void appendStatementForXref(CardXref xref, StringBuilder text, StringBuilder html) {
        Long accountId = xref.getAccountId();
        if (accountId == null) {
            return;
        }
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return;
        }
        Account account = accountOpt.get();

        Customer customer = null;
        Long custId = xref.getCustomerId();
        if (custId != null) {
            customer = customerRepository.findById(custId).orElse(null);
        }

        List<Transaction> transactions =
                transactionRepository
                        .findByCardNumber(xref.getCardNumber(), Pageable.unpaged())
                        .getContent();

        String stmtDate = DateTimeHelper.toDisplayDate(LocalDate.now());
        appendTextHeader(text, customer, account, xref.getCardNumber(), stmtDate);
        BigDecimal txTotal = appendTextTransactionLines(text, transactions);
        appendTextFooter(text, txTotal, account.getCurrentBalance());

        appendHtmlSection(html, customer, account, xref.getCardNumber(), stmtDate, transactions, txTotal);
    }

    private void appendTextHeader(
            StringBuilder out, Customer customer, Account account, String cardNumber, String stmtDate) {
        String name = formatCustomerName(customer);
        out.append(String.format("%-40s", truncate(name, 40))).append(System.lineSeparator());
        out.append(String.format("Account:%-20s", truncate(String.valueOf(account.getAcctId()), 20)))
                .append(" Card:")
                .append(String.format("%-18s", truncate(cardNumber, 16)))
                .append(System.lineSeparator());
        out.append(String.format("Statement date: %-10s", truncate(stmtDate, 10)))
                .append(System.lineSeparator());
        out.append("---------- ---------------------------------------- ------------")
                .append(System.lineSeparator());
        out.append(String.format("%-10s %-40s %12s", "Date", "Description", "Amount"))
                .append(System.lineSeparator());
        out.append("---------- ---------------------------------------- ------------")
                .append(System.lineSeparator());
    }

    private BigDecimal appendTextTransactionLines(StringBuilder out, List<Transaction> transactions) {
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (Transaction t : transactions) {
            BigDecimal amt = t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO;
            total = total.add(amt);
            String dateCol = transactionDisplayDate(t);
            String desc = transactionDescription(t);
            out.append(String.format("%-10s %-40s %12s", truncate(dateCol, 10), truncate(desc, 40), formatMoney(amt)))
                    .append(System.lineSeparator());
        }
        return total;
    }

    private void appendTextFooter(StringBuilder out, BigDecimal transactionTotal, BigDecimal currentBalance) {
        out.append("---------- ---------------------------------------- ------------")
                .append(System.lineSeparator());
        out.append(String.format("%-51s %12s", "Transaction total:", formatMoney(transactionTotal)))
                .append(System.lineSeparator());
        out.append(String.format("%-51s %12s", "Account balance:", formatMoney(nz(currentBalance))))
                .append(System.lineSeparator());
        out.append(System.lineSeparator());
    }

    private void appendHtmlSection(
            StringBuilder html,
            Customer customer,
            Account account,
            String cardNumber,
            String stmtDate,
            List<Transaction> transactions,
            BigDecimal transactionTotal) {
        String name = formatCustomerName(customer);
        html.append("<div class=\"statement\" style=\"margin-bottom:2em;border:1px solid #ccc;padding:1em;\">\n");
        html.append("<h2>").append(escapeHtml(name)).append("</h2>\n");
        html.append("<p><strong>Account:</strong> ")
                .append(escapeHtml(String.valueOf(account.getAcctId())))
                .append(" &nbsp; <strong>Card:</strong> ")
                .append(escapeHtml(cardNumber))
                .append("</p>\n");
        html.append("<p><strong>Statement date:</strong> ").append(escapeHtml(stmtDate)).append("</p>\n");
        html.append("<table border=\"1\" cellpadding=\"4\" cellspacing=\"0\">\n");
        html.append("<thead><tr><th>Date</th><th>Description</th><th style=\"text-align:right\">Amount</th></tr></thead>\n");
        html.append("<tbody>\n");
        for (Transaction t : transactions) {
            html.append("<tr><td>")
                    .append(escapeHtml(transactionDisplayDate(t)))
                    .append("</td><td>")
                    .append(escapeHtml(transactionDescription(t)))
                    .append("</td><td style=\"text-align:right\">")
                    .append(escapeHtml(formatMoney(t.getAmount())))
                    .append("</td></tr>\n");
        }
        html.append("</tbody></table>\n");
        html.append("<p><strong>Transaction total:</strong> ")
                .append(escapeHtml(formatMoney(transactionTotal)))
                .append("</p>\n");
        html.append("<p><strong>Account balance:</strong> ")
                .append(escapeHtml(formatMoney(account.getCurrentBalance())))
                .append("</p>\n");
        html.append("</div>\n");
    }

    private static String formatCustomerName(Customer c) {
        if (c == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        if (c.getFirstName() != null) {
            b.append(c.getFirstName().trim());
        }
        if (c.getMiddleName() != null && !c.getMiddleName().isBlank()) {
            if (b.length() > 0) {
                b.append(' ');
            }
            b.append(c.getMiddleName().trim());
        }
        if (c.getLastName() != null && !c.getLastName().isBlank()) {
            if (b.length() > 0) {
                b.append(' ');
            }
            b.append(c.getLastName().trim());
        }
        return b.toString();
    }

    private static String transactionDisplayDate(Transaction t) {
        String ts = t.getProcessedTimestamp();
        if (ts != null && ts.length() >= 10) {
            return ts.substring(0, 10);
        }
        ts = t.getOriginTimestamp();
        if (ts != null && ts.length() >= 10) {
            return ts.substring(0, 10);
        }
        return "";
    }

    private static String transactionDescription(Transaction t) {
        if (t.getDescription() != null && !t.getDescription().isBlank()) {
            return t.getDescription().trim();
        }
        if (t.getTypeCode() != null) {
            return "Type " + t.getTypeCode();
        }
        return "";
    }

    private static String formatMoney(BigDecimal value) {
        return MONEY.format(nz(value));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

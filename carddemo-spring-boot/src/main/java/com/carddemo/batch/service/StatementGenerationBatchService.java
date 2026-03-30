package com.carddemo.batch.service;

import com.carddemo.account.model.Account;
import com.carddemo.account.model.Customer;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.account.repository.CustomerRepository;
import com.carddemo.card.model.CardXref;
import com.carddemo.card.repository.CardXrefRepository;
import com.carddemo.transaction.model.TransactionRecord;
import com.carddemo.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatementGenerationBatchService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CardXrefRepository cardXrefRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public StatementGenerationBatchService(CardXrefRepository cardXrefRepository,
                                           CustomerRepository customerRepository,
                                           AccountRepository accountRepository,
                                           TransactionRepository transactionRepository) {
        this.cardXrefRepository = cardXrefRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public StatementGenerationResult generateStatements(LocalDate statementDate) {
        LocalDate effectiveDate = statementDate == null ? LocalDate.now() : statementDate;
        List<CardXref> xrefs = cardXrefRepository.findAll().stream()
                .sorted((a, b) -> a.getCardNumber().compareTo(b.getCardNumber()))
                .toList();

        List<String> textStatements = new ArrayList<>();
        List<String> htmlStatements = new ArrayList<>();
        List<String> accountIds = new ArrayList<>();

        for (CardXref xref : xrefs) {
            Customer customer = customerRepository.findById(xref.getCustomerId()).orElse(null);
            Account account = accountRepository.findById(xref.getAccountId()).orElse(null);
            if (customer == null || account == null) {
                continue;
            }

            List<TransactionRecord> transactions = transactionRepository.findByCardNumberOrderByProcessedTimestampAsc(xref.getCardNumber());
            BigDecimal total = transactions.stream()
                    .map(TransactionRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String text = renderTextStatement(effectiveDate, customer, account, xref, transactions, total);
            String html = renderHtmlStatement(effectiveDate, customer, account, xref, transactions, total);

            textStatements.add(text);
            htmlStatements.add(html);
            accountIds.add(account.getAccountId().toString());
        }

        Path outputDir = Path.of("/tmp/carddemo-statements");
        try {
            Files.createDirectories(outputDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize statement output directory", ex);
        }

        Path textPath = outputDir.resolve("statements-" + DATE_FMT.format(effectiveDate) + ".txt");
        Path htmlPath = outputDir.resolve("statements-" + DATE_FMT.format(effectiveDate) + ".html");
        try {
            Files.writeString(textPath, String.join(System.lineSeparator() + "---" + System.lineSeparator(), textStatements), StandardCharsets.UTF_8);
            Files.writeString(htmlPath, String.join(System.lineSeparator(), htmlStatements), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to write statement output files", ex);
        }

        return new StatementGenerationResult(textStatements.size(), textStatements, htmlStatements, textPath, htmlPath, accountIds);
    }

    private String renderTextStatement(LocalDate statementDate,
                                       Customer customer,
                                       Account account,
                                       CardXref xref,
                                       List<TransactionRecord> transactions,
                                       BigDecimal total) {
        StringBuilder sb = new StringBuilder();
        sb.append("Statement Date: ").append(DATE_FMT.format(statementDate)).append('\n');
        sb.append("Customer: ").append(customer.getFirstName().trim()).append(' ')
                .append(customer.getLastName().trim()).append('\n');
        sb.append("Account ID: ").append(account.getAccountId()).append('\n');
        sb.append("Card Number: ").append(xref.getCardNumber()).append('\n');
        sb.append("Current Balance: ").append(account.getCurrentBalance()).append('\n');
        sb.append("FICO: ").append(customer.getFicoScore()).append('\n');
        sb.append("Transactions:\n");
        for (TransactionRecord tr : transactions) {
            sb.append(" - ").append(tr.getTransactionId()).append(" | ")
                    .append(tr.getDescription()).append(" | ")
                    .append(tr.getAmount()).append('\n');
        }
        sb.append("Total statement transactions: ").append(total).append('\n');
        return sb.toString();
    }

    private String renderHtmlStatement(LocalDate statementDate,
                                       Customer customer,
                                       Account account,
                                       CardXref xref,
                                       List<TransactionRecord> transactions,
                                       BigDecimal total) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h1>CardDemo Statement</h1>");
        sb.append("<p>Date: ").append(DATE_FMT.format(statementDate)).append("</p>");
        sb.append("<p>Customer: ").append(customer.getFirstName().trim()).append(' ')
                .append(customer.getLastName().trim()).append("</p>");
        sb.append("<p>Account: ").append(account.getAccountId()).append("</p>");
        sb.append("<p>Card: ").append(xref.getCardNumber()).append("</p>");
        sb.append("<p>Current Balance: ").append(account.getCurrentBalance()).append("</p>");
        sb.append("<p>FICO: ").append(customer.getFicoScore()).append("</p>");
        sb.append("<ul>");
        for (TransactionRecord tr : transactions) {
            sb.append("<li>").append(tr.getTransactionId()).append(" - ")
                    .append(tr.getDescription()).append(" - ")
                    .append(tr.getAmount()).append("</li>");
        }
        sb.append("</ul>");
        sb.append("<p>Total statement transactions: ").append(total).append("</p>");
        sb.append("</body></html>");
        return sb.toString();
    }
}

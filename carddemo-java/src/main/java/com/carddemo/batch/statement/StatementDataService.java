package com.carddemo.batch.statement;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.account.Customer;
import com.carddemo.account.CustomerRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Replaces COBOL subroutine CBSTM03B (TRNXFILE, XREFFILE, CUSTFILE, ACCTFILE I/O) with
 * repository-backed access for statement batch jobs.
 */
@Service
public class StatementDataService {

    private final TransactionRepository transactionRepository;
    private final CardXrefRepository cardXrefRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    public StatementDataService(
            TransactionRepository transactionRepository,
            CardXrefRepository cardXrefRepository,
            CustomerRepository customerRepository,
            AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
    }

    /** Sequential / keyed transaction access by card (TRNXFILE). */
    public List<Transaction> findTransactionsByCardNumber(String cardNumber, Pageable pageable) {
        return transactionRepository.findByCardNumber(cardNumber, pageable).getContent();
    }

    /** Keyed read on XREFFILE by card number. */
    public Optional<CardXref> findXrefByCardNumber(String cardNumber) {
        return cardXrefRepository.findById(cardNumber);
    }

    /** Keyed read on CUSTFILE. */
    public Optional<Customer> findCustomerById(Long customerId) {
        return customerRepository.findById(customerId);
    }

    /** Keyed read on ACCTFILE. */
    public Optional<Account> findAccountById(Long accountId) {
        return accountRepository.findById(accountId);
    }

    /**
     * Loads account, all card cross-references for the account, the customer (from the first xref
     * when present), and all transactions for those cards.
     */
    public StatementData getStatementDataForAccount(Long accountId) {
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return new StatementData(null, null, List.of(), List.of());
        }
        Account account = accountOpt.get();
        List<CardXref> cards = cardXrefRepository.findByAccountId(accountId);
        if (cards.isEmpty()) {
            return new StatementData(account, null, List.of(), List.of());
        }
        Long customerId = cards.getFirst().getCustomerId();
        Optional<Customer> customerOpt =
                customerId != null ? customerRepository.findById(customerId) : Optional.empty();
        List<Transaction> transactions = new ArrayList<>();
        Pageable unpaged = Pageable.unpaged();
        for (CardXref xref : cards) {
            transactions.addAll(
                    transactionRepository.findByCardNumber(xref.getCardNumber(), unpaged).getContent());
        }
        return new StatementData(account, customerOpt.orElse(null), cards, transactions);
    }
}

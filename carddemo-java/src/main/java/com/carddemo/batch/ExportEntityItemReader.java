package com.carddemo.batch;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.account.Customer;
import com.carddemo.account.CustomerRepository;
import com.carddemo.card.Card;
import com.carddemo.card.CardRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionRepository;
import java.util.Iterator;
import java.util.List;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.data.domain.Sort;

/**
 * Reads entities in CBEXPORT order: customers, accounts, card xrefs, transactions, cards. Not
 * restart-persisted; suitable for full export jobs.
 */
public class ExportEntityItemReader implements ItemStreamReader<Object> {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final CardXrefRepository cardXrefRepository;
    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;

    private List<Iterator<?>> iterators;
    private int phase;
    private Iterator<?> current;

    public ExportEntityItemReader(
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            CardXrefRepository cardXrefRepository,
            TransactionRepository transactionRepository,
            CardRepository cardRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.transactionRepository = transactionRepository;
        this.cardRepository = cardRepository;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        iterators = List.of(
                customerRepository.findAll(Sort.by("custId")).iterator(),
                accountRepository.findAll(Sort.by("acctId")).iterator(),
                cardXrefRepository.findAll(Sort.by("cardNumber")).iterator(),
                transactionRepository.findAll(Sort.by("transactionId")).iterator(),
                cardRepository.findAll(Sort.by("cardNumber")).iterator());
        phase = 0;
        current = iterators.isEmpty() ? null : iterators.getFirst();
    }

    @Override
    public Object read() {
        while (current != null) {
            if (current.hasNext()) {
                return current.next();
            }
            phase++;
            if (phase >= iterators.size()) {
                current = null;
                return null;
            }
            current = iterators.get(phase);
        }
        return null;
    }

    @Override
    public void update(ExecutionContext executionContext) {
        // Non-restartable reader
    }

    @Override
    public void close() throws ItemStreamException {
        iterators = null;
        current = null;
        phase = 0;
    }
}

package com.carddemo.batch;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Validates each daily transaction against card cross-reference and account (CBTRN01C). Invalid
 * rows are logged and filtered out ({@code null} return); valid rows pass through unchanged.
 */
@Component
public class DailyTransactionProcessor implements ItemProcessor<DailyTransaction, DailyTransaction> {

    private static final Logger log = LoggerFactory.getLogger(DailyTransactionProcessor.class);

    private final CardXrefRepository cardXrefRepository;
    private final AccountRepository accountRepository;

    public DailyTransactionProcessor(
            CardXrefRepository cardXrefRepository, AccountRepository accountRepository) {
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public DailyTransaction process(DailyTransaction item) {
        String txId = item.transactionId() == null ? "" : item.transactionId().trim();
        String cardNum = item.cardNumber() == null ? "" : item.cardNumber().trim();
        if (cardNum.isEmpty()) {
            log.warn(
                    "CARD NUMBER (blank) COULD NOT BE VERIFIED. SKIPPING TRANSACTION ID-{}",
                    txId);
            return null;
        }

        Optional<CardXref> xrefOpt = cardXrefRepository.findById(cardNum);
        if (xrefOpt.isEmpty()) {
            log.warn(
                    "CARD NUMBER {} COULD NOT BE VERIFIED. SKIPPING TRANSACTION ID-{}",
                    cardNum,
                    txId);
            return null;
        }

        Long acctId = xrefOpt.get().getAccountId();
        if (acctId == null) {
            log.warn(
                    "CARD NUMBER {} COULD NOT BE VERIFIED (no account on xref). SKIPPING TRANSACTION ID-{}",
                    cardNum,
                    txId);
            return null;
        }

        Optional<Account> accountOpt = accountRepository.findById(acctId);
        if (accountOpt.isEmpty()) {
            log.warn("ACCOUNT {} NOT FOUND (transactionId={} cardNumber={})", acctId, txId, cardNum);
            return null;
        }

        return item;
    }
}

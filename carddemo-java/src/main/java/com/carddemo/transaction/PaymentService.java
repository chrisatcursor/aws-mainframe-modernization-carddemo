package com.carddemo.transaction;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.common.DateTimeHelper;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final AccountRepository accountRepository;
    private final CardXrefRepository cardXrefRepository;
    private final TransactionRepository transactionRepository;

    public PaymentService(
            AccountRepository accountRepository,
            CardXrefRepository cardXrefRepository,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Pays the full account balance as a bill payment (COBIL00C): type 02, category 2, source BILLPAY.
     */
    @Transactional
    public PaymentResult payBill(Long acctId) {
        Account account = accountRepository
                .findById(acctId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + acctId));

        BigDecimal balance = account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO;
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentResult.error("Account has no positive balance to pay.");
        }

        List<CardXref> xrefs = cardXrefRepository.findByAccountId(acctId);
        if (xrefs.isEmpty()) {
            return PaymentResult.error("No card on file for this account.");
        }
        String cardNumber = xrefs.get(0).getCardNumber();

        Long nextId = transactionRepository.getNextTransactionId();
        String transactionId = String.format("%016d", nextId);

        String now = DateTimeHelper.toTimestamp(LocalDateTime.now());
        Transaction txn = TransactionFactory.newTransaction();
        txn.setTransactionId(transactionId);
        txn.setTypeCode("02");
        txn.setCategoryCode(2);
        txn.setSource("BILLPAY");
        txn.setDescription("Bill Payment");
        txn.setAmount(balance);
        txn.setCardNumber(cardNumber);
        txn.setOriginTimestamp(now);
        txn.setProcessedTimestamp(now);

        account.setCurrentBalance(BigDecimal.ZERO);

        transactionRepository.save(txn);
        accountRepository.save(account);

        return PaymentResult.success(transactionId, balance);
    }
}

package com.carddemo.batch;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.common.DateTimeHelper;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionCategoryBalance;
import com.carddemo.transaction.TransactionCategoryBalance.TransactionCategoryBalanceKey;
import com.carddemo.transaction.TransactionCategoryBalanceRepository;
import com.carddemo.transaction.TransactionFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class TransactionPostingProcessor implements ItemProcessor<DailyTransactionRecord, Transaction> {

    private static final Logger log = LoggerFactory.getLogger(TransactionPostingProcessor.class);

    private static final String ACTIVE_STATUS = "Y";
    private static final DateTimeFormatter EXPIRATION_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AccountRepository accountRepository;
    private final CardXrefRepository cardXrefRepository;
    private final TransactionCategoryBalanceRepository transactionCategoryBalanceRepository;

    public TransactionPostingProcessor(
            AccountRepository accountRepository,
            CardXrefRepository cardXrefRepository,
            TransactionCategoryBalanceRepository transactionCategoryBalanceRepository) {
        this.accountRepository = accountRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.transactionCategoryBalanceRepository = transactionCategoryBalanceRepository;
    }

    @Override
    public Transaction process(DailyTransactionRecord item) {
        String txId = item.transactionId();
        String cardNum = item.cardNumber();

        Optional<CardXref> xrefOpt = cardXrefRepository.findById(cardNum);
        if (xrefOpt.isEmpty()) {
            reject(txId, cardNum, "XREF_NOT_FOUND", "No card cross-reference for card number");
            return null;
        }
        Long acctId = xrefOpt.get().getAccountId();
        if (acctId == null) {
            reject(txId, cardNum, "XREF_NO_ACCOUNT", "Card cross-reference has no account id");
            return null;
        }

        Optional<Account> accountOpt = accountRepository.findById(acctId);
        if (accountOpt.isEmpty()) {
            reject(txId, cardNum, "ACCOUNT_NOT_FOUND", "Account does not exist");
            return null;
        }
        Account account = accountOpt.get();

        if (!ACTIVE_STATUS.equals(account.getActiveStatus())) {
            reject(txId, cardNum, "INACTIVE_ACCOUNT", "Account is not active");
            return null;
        }

        LocalDate today = LocalDate.now();
        String expRaw = account.getExpirationDate();
        if (expRaw != null && !expRaw.isBlank()) {
            LocalDate expiration = parseExpiration(expRaw);
            if (expiration == null) {
                reject(txId, cardNum, "BAD_EXPIRATION", "Account expiration date is not parseable");
                return null;
            }
            if (today.isAfter(expiration)) {
                reject(txId, cardNum, "ACCOUNT_EXPIRED", "Account is past expiration date");
                return null;
            }
        }

        BigDecimal amount = item.amount() == null ? BigDecimal.ZERO : item.amount();
        BigDecimal currentBal = nz(account.getCurrentBalance());
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal creditLimit = account.getCreditLimit();
            if (creditLimit != null && currentBal.add(amount).compareTo(creditLimit) > 0) {
                reject(txId, cardNum, "OVER_LIMIT", "Debit would exceed credit limit");
                return null;
            }
        }

        String typeCode = item.typeCode() == null ? "" : item.typeCode();
        applyCategoryBalance(acctId, typeCode, item.categoryCode(), amount);
        applyAccountMovement(account, amount);
        accountRepository.save(account);

        return buildPostedTransaction(item, txId, typeCode);
    }

    private static Transaction buildPostedTransaction(
            DailyTransactionRecord item, String txId, String typeCode) {
        Transaction tx = TransactionFactory.newTransaction();
        tx.setTransactionId(txId);
        tx.setTypeCode(typeCode);
        tx.setCategoryCode(item.categoryCode());
        tx.setSource("DAILY");
        String desc = item.merchantName();
        tx.setDescription(desc == null ? "" : desc);
        tx.setAmount(item.amount());
        tx.setMerchantId(item.merchantId());
        tx.setMerchantName(item.merchantName());
        tx.setMerchantCity(item.merchantCity());
        tx.setMerchantZip(item.merchantZip());
        tx.setCardNumber(item.cardNumber());
        tx.setOriginTimestamp(item.originTimestamp());
        tx.setProcessedTimestamp(DateTimeHelper.toTimestamp(LocalDateTime.now()));
        return tx;
    }

    private void applyCategoryBalance(Long acctId, String typeCode, int categoryCode, BigDecimal amount) {
        TransactionCategoryBalanceKey key = new TransactionCategoryBalanceKey(acctId, typeCode, categoryCode);
        Optional<TransactionCategoryBalance> existing = transactionCategoryBalanceRepository.findById(key);
        if (existing.isEmpty()) {
            TransactionCategoryBalance created =
                    TransactionCategoryBalance.newWithIdAndBalance(key, amount);
            transactionCategoryBalanceRepository.save(created);
        } else {
            TransactionCategoryBalance row = existing.get();
            row.setBalance(nz(row.getBalance()).add(amount));
            transactionCategoryBalanceRepository.save(row);
        }
    }

    private static void applyAccountMovement(Account account, BigDecimal amount) {
        account.setCurrentBalance(nz(account.getCurrentBalance()).add(amount));
        int cmp = amount.compareTo(BigDecimal.ZERO);
        if (cmp > 0) {
            account.setCurrentCycleDebit(nz(account.getCurrentCycleDebit()).add(amount));
        } else if (cmp < 0) {
            account.setCurrentCycleCredit(nz(account.getCurrentCycleCredit()).add(amount.abs()));
        }
    }

    private static LocalDate parseExpiration(String expirationDate) {
        try {
            return LocalDate.parse(expirationDate.trim(), EXPIRATION_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        }
        return v;
    }

    private void reject(String transactionId, String cardNumber, String reasonCode, String reasonMessage) {
        RejectionRecord rejection = new RejectionRecord(transactionId, cardNumber, reasonCode, reasonMessage);
        log.warn("Rejected daily transaction: {}", rejection);
    }
}

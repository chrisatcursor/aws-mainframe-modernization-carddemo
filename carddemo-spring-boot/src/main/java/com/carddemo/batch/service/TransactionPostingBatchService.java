package com.carddemo.batch.service;

import com.carddemo.account.model.Account;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.batch.model.DailyTransactionRejection;
import com.carddemo.batch.repository.DailyTransactionRejectionRepository;
import com.carddemo.card.model.CardXref;
import com.carddemo.card.repository.CardXrefRepository;
import com.carddemo.transaction.model.DailyTransactionRecord;
import com.carddemo.transaction.model.TransactionCategoryBalance;
import com.carddemo.transaction.model.TransactionCategoryBalanceId;
import com.carddemo.transaction.model.TransactionRecord;
import com.carddemo.transaction.repository.DailyTransactionRepository;
import com.carddemo.transaction.repository.TransactionCategoryBalanceRepository;
import com.carddemo.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionPostingBatchService {

    private final DailyTransactionRepository dailyTransactionRepository;
    private final TransactionRepository transactionRepository;
    private final CardXrefRepository cardXrefRepository;
    private final AccountRepository accountRepository;
    private final TransactionCategoryBalanceRepository transactionCategoryBalanceRepository;
    private final DailyTransactionRejectionRepository rejectionRepository;

    public TransactionPostingBatchService(DailyTransactionRepository dailyTransactionRepository,
                                         TransactionRepository transactionRepository,
                                         CardXrefRepository cardXrefRepository,
                                         AccountRepository accountRepository,
                                         TransactionCategoryBalanceRepository transactionCategoryBalanceRepository,
                                         DailyTransactionRejectionRepository rejectionRepository) {
        this.dailyTransactionRepository = dailyTransactionRepository;
        this.transactionRepository = transactionRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
        this.transactionCategoryBalanceRepository = transactionCategoryBalanceRepository;
        this.rejectionRepository = rejectionRepository;
    }

    @Transactional
    public BatchPostingResult processPostingBatch(LocalDate processingDate) {
        List<DailyTransactionRecord> dailyRecords = dailyTransactionRepository.findAll();
        int processed = dailyRecords.size();
        int posted = 0;
        int rejected = 0;

        for (DailyTransactionRecord daily : dailyRecords) {
            ValidationOutcome validation = validate(daily);
            if (!validation.accepted()) {
                DailyTransactionRejection rejection = new DailyTransactionRejection();
                rejection.setTransactionId(daily.getTransactionId());
                rejection.setReasonCode(validation.reasonCode());
                rejection.setReasonDescription(validation.reasonDescription());
                rejectionRepository.save(rejection);
                rejected++;
                continue;
            }

            Account account = validation.account();
            TransactionRecord record = mapToTransactionRecord(daily);
            transactionRepository.save(record);

            updateAccountBalance(account, daily.getAmount());
            accountRepository.save(account);

            updateCategoryBalance(account.getAccountId(), daily);
            posted++;
        }

        int returnCode = rejected > 0 ? 4 : 0;
        return new BatchPostingResult(processed, posted, rejected, returnCode);
    }

    private ValidationOutcome validate(DailyTransactionRecord daily) {
        Optional<CardXref> xrefOpt = cardXrefRepository.findById(daily.getCardNumber());
        if (xrefOpt.isEmpty()) {
            return ValidationOutcome.rejected(100, "INVALID CARD NUMBER");
        }

        CardXref xref = xrefOpt.get();
        Optional<Account> accountOpt = accountRepository.findById(xref.getAccountId());
        if (accountOpt.isEmpty()) {
            return ValidationOutcome.rejected(101, "ACCOUNT RECORD NOT FOUND");
        }

        Account account = accountOpt.get();

        BigDecimal projected = account.getCurrentCycleCredit()
                .subtract(account.getCurrentCycleDebit())
                .add(daily.getAmount());
        if (projected.compareTo(account.getCreditLimit()) > 0) {
            return ValidationOutcome.rejected(102, "OVERLIMIT TRANSACTION");
        }

        if (daily.getOriginalTimestamp() != null
                && account.getExpirationDate() != null
                && daily.getOriginalTimestamp().toLocalDate().isAfter(account.getExpirationDate())) {
            return ValidationOutcome.rejected(103, "TRANSACTION RECEIVED AFTER ACCT EXPIRATION");
        }

        return ValidationOutcome.accepted(account);
    }

    private TransactionRecord mapToTransactionRecord(DailyTransactionRecord daily) {
        TransactionRecord record = new TransactionRecord();
        record.setTransactionId(daily.getTransactionId());
        record.setTransactionTypeCode(daily.getTransactionTypeCode());
        record.setTransactionCategoryCode(daily.getTransactionCategoryCode());
        record.setSource(daily.getSource());
        record.setDescription(daily.getDescription());
        record.setAmount(daily.getAmount());
        record.setMerchantId(daily.getMerchantId());
        record.setMerchantName(daily.getMerchantName());
        record.setMerchantCity(daily.getMerchantCity());
        record.setMerchantZip(daily.getMerchantZip());
        record.setCardNumber(daily.getCardNumber());
        record.setOriginalTimestamp(daily.getOriginalTimestamp());
        record.setProcessedTimestamp(daily.getProcessedTimestamp());
        return record;
    }

    private void updateAccountBalance(Account account, BigDecimal amount) {
        account.setCurrentBalance(account.getCurrentBalance().add(amount));
        if (amount.signum() >= 0) {
            account.setCurrentCycleCredit(account.getCurrentCycleCredit().add(amount));
        } else {
            account.setCurrentCycleDebit(account.getCurrentCycleDebit().add(amount.abs()));
        }
    }

    private void updateCategoryBalance(Long accountId, DailyTransactionRecord daily) {
        TransactionCategoryBalanceId id = new TransactionCategoryBalanceId(
                accountId,
                daily.getTransactionTypeCode(),
                daily.getTransactionCategoryCode());

        TransactionCategoryBalance balance = transactionCategoryBalanceRepository.findById(id)
                .orElseGet(() -> {
                    TransactionCategoryBalance created = new TransactionCategoryBalance();
                    created.setId(id);
                    created.setBalance(BigDecimal.ZERO);
                    return created;
                });
        balance.setBalance(balance.getBalance().add(daily.getAmount()));
        transactionCategoryBalanceRepository.save(balance);
    }

    private record ValidationOutcome(boolean accepted, int reasonCode, String reasonDescription, Account account) {
        static ValidationOutcome accepted(Account account) {
            return new ValidationOutcome(true, 0, "", account);
        }

        static ValidationOutcome rejected(int reasonCode, String reasonDescription) {
            return new ValidationOutcome(false, reasonCode, reasonDescription, null);
        }
    }
}

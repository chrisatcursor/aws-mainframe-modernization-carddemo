package com.carddemo.batch.service;

import com.carddemo.account.model.Account;
import com.carddemo.account.repository.AccountRepository;
import com.carddemo.card.model.CardXref;
import com.carddemo.card.repository.CardXrefRepository;
import com.carddemo.transaction.model.DisclosureGroup;
import com.carddemo.transaction.model.TransactionCategoryBalance;
import com.carddemo.transaction.model.TransactionRecord;
import com.carddemo.transaction.repository.DisclosureGroupRepository;
import com.carddemo.transaction.repository.TransactionCategoryBalanceRepository;
import com.carddemo.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class InterestCalculationBatchService {

    private static final DateTimeFormatter TRAN_ID_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String DEFAULT_GROUP_ID = "DEFAULT";

    private final TransactionCategoryBalanceRepository balanceRepository;
    private final AccountRepository accountRepository;
    private final CardXrefRepository cardXrefRepository;
    private final DisclosureGroupRepository disclosureGroupRepository;
    private final TransactionRepository transactionRepository;

    public InterestCalculationBatchService(TransactionCategoryBalanceRepository balanceRepository,
                                          AccountRepository accountRepository,
                                          CardXrefRepository cardXrefRepository,
                                          DisclosureGroupRepository disclosureGroupRepository,
                                          TransactionRepository transactionRepository) {
        this.balanceRepository = balanceRepository;
        this.accountRepository = accountRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.disclosureGroupRepository = disclosureGroupRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public InterestCalculationBatchResult calculateInterest(String parmDate) {
        AtomicInteger suffix = new AtomicInteger(0);
        List<TransactionCategoryBalance> balances = balanceRepository.findAll().stream()
                .sorted((a, b) -> {
                    int cmp = a.getId().getAccountId().compareTo(b.getId().getAccountId());
                    if (cmp != 0) {
                        return cmp;
                    }
                    cmp = a.getId().getTransactionTypeCode().compareTo(b.getId().getTransactionTypeCode());
                    if (cmp != 0) {
                        return cmp;
                    }
                    return a.getId().getTransactionCategoryCode().compareTo(b.getId().getTransactionCategoryCode());
                })
                .toList();

        Map<Long, List<TransactionCategoryBalance>> byAccount = balances.stream()
                .collect(Collectors.groupingBy(b -> b.getId().getAccountId()));

        int accountsUpdated = 0;
        int transactionsWritten = 0;

        for (Map.Entry<Long, List<TransactionCategoryBalance>> entry : byAccount.entrySet()) {
            Long accountId = entry.getKey();
            Account account = accountRepository.findById(accountId).orElse(null);
            if (account == null) {
                continue;
            }

            BigDecimal totalInterest = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            String groupId = normalizeGroupId(account.getGroupId());
            String cardNumber = resolveCardNumber(accountId).orElse("0000000000000000");

            for (TransactionCategoryBalance balance : entry.getValue()) {
                BigDecimal interestRate = resolveInterestRate(groupId,
                        balance.getId().getTransactionTypeCode(),
                        balance.getId().getTransactionCategoryCode());

                if (interestRate == null || interestRate.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                BigDecimal monthlyInterest = balance.getBalance()
                        .multiply(interestRate)
                        .divide(BigDecimal.valueOf(1200), 2, RoundingMode.HALF_UP);

                if (monthlyInterest.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }

                totalInterest = totalInterest.add(monthlyInterest).setScale(2, RoundingMode.HALF_UP);
                writeInterestTransaction(parmDate, suffix.incrementAndGet(), accountId, cardNumber, monthlyInterest);
                transactionsWritten++;
            }

            if (totalInterest.compareTo(BigDecimal.ZERO) != 0) {
                account.setCurrentBalance(account.getCurrentBalance().add(totalInterest).setScale(2, RoundingMode.HALF_UP));
                account.setCurrentCycleCredit(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                account.setCurrentCycleDebit(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                accountRepository.save(account);
                accountsUpdated++;
            }
        }

        return new InterestCalculationBatchResult(balances.size(), transactionsWritten, suffix.get());
    }

    private Optional<String> resolveCardNumber(Long accountId) {
        return cardXrefRepository.findByAccountIdOrderByCardNumberAsc(accountId).stream()
                .map(CardXref::getCardNumber)
                .findFirst();
    }

    private String normalizeGroupId(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return DEFAULT_GROUP_ID;
        }
        return groupId.trim();
    }

    private BigDecimal resolveInterestRate(String groupId, String typeCode, Integer categoryCode) {
        Optional<DisclosureGroup> primary = disclosureGroupRepository
                .findByIdAccountGroupIdAndIdTransactionTypeCodeAndIdTransactionCategoryCode(groupId, typeCode, categoryCode);
        if (primary.isPresent()) {
            return primary.get().getInterestRate();
        }

        return disclosureGroupRepository
                .findByIdAccountGroupIdAndIdTransactionTypeCodeAndIdTransactionCategoryCode(DEFAULT_GROUP_ID, typeCode, categoryCode)
                .map(DisclosureGroup::getInterestRate)
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    private void writeInterestTransaction(String parmDate,
                                          int suffix,
                                          Long accountId,
                                          String cardNumber,
                                          BigDecimal amount) {
        TransactionRecord tx = new TransactionRecord();
        tx.setTransactionId(formatTransactionId(parmDate, suffix));
        tx.setTransactionTypeCode("01");
        tx.setTransactionCategoryCode(5);
        tx.setSource("System");
        tx.setDescription("Int. for a/c " + accountId);
        tx.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        tx.setMerchantId(0L);
        tx.setMerchantName("");
        tx.setMerchantCity("");
        tx.setMerchantZip("");
        tx.setCardNumber(cardNumber);
        LocalDateTime now = LocalDateTime.now();
        tx.setOriginalTimestamp(now);
        tx.setProcessedTimestamp(now);
        transactionRepository.save(tx);
    }

    private String formatTransactionId(String parmDate, int suffix) {
        String datePart = normalizeParmDate(parmDate);
        String suffixPart = String.format("%06d", suffix);
        return (datePart + suffixPart).substring(0, 16);
    }

    private String normalizeParmDate(String parmDate) {
        if (parmDate == null || parmDate.isBlank()) {
            return TRAN_ID_DATE.format(LocalDate.now());
        }
        String trimmed = parmDate.trim();
        return trimmed.length() >= 10 ? trimmed.substring(0, 10) : String.format("%-10s", trimmed);
    }
}

package com.carddemo.batch;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.common.DateTimeHelper;
import com.carddemo.transaction.DisclosureGroup;
import com.carddemo.transaction.DisclosureGroup.DisclosureGroupKey;
import com.carddemo.transaction.DisclosureGroupRepository;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionCategoryBalance;
import com.carddemo.transaction.TransactionCategoryBalanceRepository;
import com.carddemo.transaction.TransactionFactory;
import com.carddemo.transaction.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Monthly interest accrual migrated from CBACT04C: reads transaction category balances,
 * applies disclosure (or default) annual rates, posts one interest transaction per account,
 * and updates {@link Account#getCurrentBalance()}.
 */
@Service
public class InterestCalculationService {

    private static final Logger log = LoggerFactory.getLogger(InterestCalculationService.class);

    private static final String INTEREST_TYPE_CODE = "05";
    private static final int INTEREST_CATEGORY_CODE = 0;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    private final AccountRepository accountRepository;
    private final TransactionCategoryBalanceRepository transactionCategoryBalanceRepository;
    private final DisclosureGroupRepository disclosureGroupRepository;
    private final TransactionRepository transactionRepository;
    private final BigDecimal defaultAnnualRatePercent;
    private final TransactionTemplate requiresNewTemplate;

    public InterestCalculationService(
            AccountRepository accountRepository,
            TransactionCategoryBalanceRepository transactionCategoryBalanceRepository,
            DisclosureGroupRepository disclosureGroupRepository,
            TransactionRepository transactionRepository,
            PlatformTransactionManager transactionManager,
            @Value("${carddemo.interest.default-rate:18.99}") BigDecimal defaultAnnualRatePercent) {
        this.accountRepository = accountRepository;
        this.transactionCategoryBalanceRepository = transactionCategoryBalanceRepository;
        this.disclosureGroupRepository = disclosureGroupRepository;
        this.transactionRepository = transactionRepository;
        this.defaultAnnualRatePercent = defaultAnnualRatePercent.setScale(2, RoundingMode.HALF_UP);
        this.requiresNewTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Processes every account: sums monthly interest from category balances, updates balance,
     * and persists an interest transaction when the total is non-zero.
     */
    public void calculateInterest() {
        List<Account> accounts = accountRepository.findAll();
        for (Account account : accounts) {
            Long acctId = account.getAcctId();
            try {
                requiresNewTemplate.executeWithoutResult(status -> processOneAccount(acctId));
            } catch (Exception e) {
                log.error("Interest calculation failed for account {}; continuing with next account", acctId, e);
            }
        }
    }

    private void processOneAccount(Long acctId) {
        Account account =
                accountRepository.findById(acctId).orElseThrow(() -> new IllegalStateException("Missing account: " + acctId));

        List<TransactionCategoryBalance> rows = transactionCategoryBalanceRepository.findByIdAcctId(acctId);
        BigDecimal totalInterest = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

        for (TransactionCategoryBalance row : rows) {
            BigDecimal balance = nz(row.getBalance());
            if (balance.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            String typeCode = row.getId().getTypeCode();
            Integer categoryCode = row.getId().getCategoryCode();
            BigDecimal annualRate = resolveAnnualRatePercent(account.getGroupId(), typeCode, categoryCode);
            BigDecimal lineInterest = monthlyInterestForLine(balance, annualRate);
            totalInterest = totalInterest.add(lineInterest);
        }

        totalInterest = totalInterest.setScale(2, RoundingMode.HALF_UP);
        if (totalInterest.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        account.setCurrentBalance(nz(account.getCurrentBalance()).add(totalInterest));
        accountRepository.save(account);

        Transaction tx = TransactionFactory.newTransaction();
        tx.setTransactionId(newInterestTransactionId());
        tx.setTypeCode(INTEREST_TYPE_CODE);
        tx.setCategoryCode(INTEREST_CATEGORY_CODE);
        tx.setSource("INTEREST");
        tx.setDescription("INTEREST");
        tx.setAmount(totalInterest);
        tx.setProcessedTimestamp(DateTimeHelper.toTimestamp(LocalDateTime.now()));
        transactionRepository.save(tx);
    }

    private BigDecimal resolveAnnualRatePercent(String groupId, String typeCode, Integer categoryCode) {
        if (groupId == null || groupId.isBlank() || typeCode == null || categoryCode == null) {
            return defaultAnnualRatePercent;
        }
        Optional<DisclosureGroup> found =
                disclosureGroupRepository.findById(new DisclosureGroupKey(groupId, typeCode, categoryCode));
        return found.map(DisclosureGroup::getInterestRate)
                .map(r -> r.setScale(2, RoundingMode.HALF_UP))
                .orElse(defaultAnnualRatePercent);
    }

    private static BigDecimal monthlyInterestForLine(BigDecimal balance, BigDecimal annualRatePercent) {
        return balance.multiply(annualRatePercent)
                .divide(HUNDRED, 10, RoundingMode.HALF_UP)
                .divide(TWELVE, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        }
        return v;
    }

    private static String newInterestTransactionId() {
        return String.format("INT%013d", System.nanoTime() % 10000000000000L);
    }
}

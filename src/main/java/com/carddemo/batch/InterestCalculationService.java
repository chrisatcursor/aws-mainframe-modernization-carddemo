package com.carddemo.batch;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.DisclosureGroup;
import com.carddemo.transaction.DisclosureGroupRepository;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionCategoryBalance;
import com.carddemo.transaction.TransactionCategoryBalanceRepository;
import com.carddemo.transaction.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class InterestCalculationService {

    private final TransactionCategoryBalanceRepository tcatRepository;
    private final AccountRepository accountRepository;
    private final CardXrefRepository cardXrefRepository;
    private final DisclosureGroupRepository disclosureGroupRepository;
    private final TransactionRepository transactionRepository;

    public InterestCalculationService(
            TransactionCategoryBalanceRepository tcatRepository,
            AccountRepository accountRepository,
            CardXrefRepository cardXrefRepository,
            DisclosureGroupRepository disclosureGroupRepository,
            TransactionRepository transactionRepository) {
        this.tcatRepository = tcatRepository;
        this.accountRepository = accountRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.disclosureGroupRepository = disclosureGroupRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void run(String parmDate) {
        List<TransactionCategoryBalance> all = tcatRepository.findAll();
        all.sort(Comparator
                .comparing((TransactionCategoryBalance t) -> t.getId().getAccountId())
                .thenComparing(t -> t.getId().getTypeCode())
                .thenComparing(t -> t.getId().getCategoryCode()));

        Long prevAcct = null;
        BigDecimal totalInt = BigDecimal.ZERO;
        AtomicInteger suffix = new AtomicInteger(0);

        for (TransactionCategoryBalance row : all) {
            long acctId = row.getId().getAccountId();
            if (prevAcct != null && prevAcct != acctId) {
                flushAccount(prevAcct, totalInt, parmDate, suffix);
                totalInt = BigDecimal.ZERO;
            }
            prevAcct = acctId;

            Optional<Account> accOpt = accountRepository.findById(acctId);
            if (accOpt.isEmpty()) {
                continue;
            }
            Account acct = accOpt.get();
            BigDecimal rate = resolveRate(acct.getGroupId(), row.getId().getTypeCode(), row.getId().getCategoryCode());
            if (rate.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal bal = row.getBalance() == null ? BigDecimal.ZERO : row.getBalance();
            BigDecimal monthly = bal.multiply(rate).divide(new BigDecimal("1200"), 2, RoundingMode.HALF_UP);
            totalInt = totalInt.add(monthly);
        }
        if (prevAcct != null) {
            flushAccount(prevAcct, totalInt, parmDate, suffix);
        }
    }

    private BigDecimal resolveRate(String groupId, String typeCd, int catCd) {
        String gid = groupId == null || groupId.isBlank() ? "DEFAULT" : groupId;
        DisclosureGroup.DisclosureGroupKey k = new DisclosureGroup.DisclosureGroupKey();
        k.setAccountGroupId(gid);
        k.setTranTypeCode(typeCd);
        k.setTranCategoryCode(catCd);
        Optional<DisclosureGroup> dgOpt = disclosureGroupRepository.findById(k);
        if (dgOpt.isPresent() && dgOpt.get().getInterestRate() != null) {
            return dgOpt.get().getInterestRate();
        }
        DisclosureGroup.DisclosureGroupKey def = new DisclosureGroup.DisclosureGroupKey();
        def.setAccountGroupId("DEFAULT");
        def.setTranTypeCode(typeCd);
        def.setTranCategoryCode(catCd);
        return disclosureGroupRepository.findById(def).map(DisclosureGroup::getInterestRate).orElse(BigDecimal.ZERO);
    }

    private void flushAccount(long acctId, BigDecimal totalInt, String parmDate, AtomicInteger suffix) {
        if (totalInt.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        Optional<Account> accOpt = accountRepository.findById(acctId);
        if (accOpt.isEmpty()) {
            return;
        }
        Account acct = accOpt.get();
        acct.setCurrentBalance(nz(acct.getCurrentBalance()).add(totalInt));
        acct.setCurrentCycleCredit(BigDecimal.ZERO);
        acct.setCurrentCycleDebit(BigDecimal.ZERO);
        accountRepository.save(acct);

        Optional<CardXref> xrefOpt = cardXrefRepository.findByAccountId(acctId);
        String cardNum = xrefOpt.map(CardXref::getCardNumber).orElse("0000000000000000");

        String base = parmDate == null || parmDate.isBlank() ? "2000-01-01" : parmDate.trim();
        String tranId = padRight(base + String.format("%06d", suffix.incrementAndGet()), 16);

        Transaction t = new Transaction();
        t.setId(tranId);
        t.setTypeCode("01");
        t.setCategoryCode(5);
        t.setSource("System");
        t.setDescription("Int. for a/c " + acctId);
        t.setAmount(totalInt);
        t.setMerchantId(0L);
        t.setMerchantName("");
        t.setMerchantCity("");
        t.setMerchantZip("");
        t.setCardNumber(cardNum);
        String ts = Db2FormatTimestamp.now();
        t.setOrigTimestamp(ts);
        t.setProcTimestamp(ts);
        transactionRepository.save(t);
    }

    private static BigDecimal nz(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }

    private static String padRight(String s, int len) {
        if (s.length() >= len) {
            return s.substring(0, len);
        }
        return s + " ".repeat(len - s.length());
    }
}

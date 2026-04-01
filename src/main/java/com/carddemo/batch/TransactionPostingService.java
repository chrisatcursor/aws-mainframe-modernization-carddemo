package com.carddemo.batch;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionCategoryBalance;
import com.carddemo.transaction.TransactionCategoryBalanceRepository;
import com.carddemo.transaction.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionPostingService {

    private final CardXrefRepository cardXrefRepository;
    private final AccountRepository accountRepository;
    private final TransactionCategoryBalanceRepository tcatRepository;
    private final TransactionRepository transactionRepository;

    public TransactionPostingService(
            CardXrefRepository cardXrefRepository,
            AccountRepository accountRepository,
            TransactionCategoryBalanceRepository tcatRepository,
            TransactionRepository transactionRepository) {
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
        this.tcatRepository = tcatRepository;
        this.transactionRepository = transactionRepository;
    }

    public PostingResult postFromFile(Path inputFile, Path rejectFile) throws IOException {
        List<String> lines = Files.readAllLines(inputFile, StandardCharsets.UTF_8);
        int processed = 0;
        int rejected = 0;
        Files.createDirectories(rejectFile.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(rejectFile, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }
                String padded = pad(line, 350);
                DalyTranRecordParser.ParsedDalyTran p;
                try {
                    p = DalyTranRecordParser.parse(padded);
                } catch (Exception ex) {
                    writeReject(w, padded, 999, "PARSE ERROR");
                    rejected++;
                    continue;
                }
                try {
                    boolean ok = postOne(p);
                    if (ok) {
                        processed++;
                    } else {
                        writeReject(w, padded, 100, "VALIDATION FAILED");
                        rejected++;
                    }
                } catch (Exception ex) {
                    writeReject(w, padded, 888, "PROCESSING ERROR");
                    rejected++;
                }
            }
        }
        return new PostingResult(processed, rejected);
    }

    @Transactional
    public boolean postOne(DalyTranRecordParser.ParsedDalyTran p) {
        Optional<CardXref> xrefOpt = cardXrefRepository.findById(p.cardNumber());
        if (xrefOpt.isEmpty()) {
            return false;
        }
        CardXref xref = xrefOpt.get();
        Optional<Account> accOpt = accountRepository.findById(xref.getAccountId());
        if (accOpt.isEmpty()) {
            return false;
        }
        Account acct = accOpt.get();
        BigDecimal tempBal = nz(acct.getCurrentCycleCredit())
                .subtract(nz(acct.getCurrentCycleDebit()))
                .add(p.amount());
        if (acct.getCreditLimit() != null && acct.getCreditLimit().compareTo(tempBal) < 0) {
            return false;
        }
        String exp = acct.getExpirationDate();
        String origPrefix = p.origTimestamp().length() >= 10 ? p.origTimestamp().substring(0, 10) : "";
        if (exp != null && exp.length() >= 10 && origPrefix.length() == 10) {
            if (origPrefix.compareTo(exp.substring(0, 10)) > 0) {
                return false;
            }
        }

        var key = new TransactionCategoryBalance.TransactionCategoryBalanceKey();
        key.setAccountId(xref.getAccountId());
        key.setTypeCode(p.typeCode());
        key.setCategoryCode(p.categoryCode());
        Optional<TransactionCategoryBalance> tcbOpt = tcatRepository.findById(key);
        TransactionCategoryBalance tcb = tcbOpt.orElseGet(() -> {
            TransactionCategoryBalance n = new TransactionCategoryBalance();
            n.setId(key);
            n.setBalance(BigDecimal.ZERO);
            return n;
        });
        tcb.setBalance(nz(tcb.getBalance()).add(p.amount()));
        tcatRepository.save(tcb);

        acct.setCurrentBalance(nz(acct.getCurrentBalance()).add(p.amount()));
        if (p.amount().compareTo(BigDecimal.ZERO) >= 0) {
            acct.setCurrentCycleCredit(nz(acct.getCurrentCycleCredit()).add(p.amount()));
        } else {
            acct.setCurrentCycleDebit(nz(acct.getCurrentCycleDebit()).add(p.amount()));
        }
        accountRepository.save(acct);

        Transaction t = new Transaction();
        t.setId(p.id());
        t.setTypeCode(p.typeCode());
        t.setCategoryCode(p.categoryCode());
        t.setSource(p.source());
        t.setDescription(p.description());
        t.setAmount(p.amount());
        t.setMerchantId(p.merchantId());
        t.setMerchantName(p.merchantName());
        t.setMerchantCity(p.merchantCity());
        t.setMerchantZip(p.merchantZip());
        t.setCardNumber(p.cardNumber());
        t.setOrigTimestamp(p.origTimestamp());
        t.setProcTimestamp(Db2FormatTimestamp.now());
        transactionRepository.save(t);
        return true;
    }

    private static void writeReject(BufferedWriter w, String tran350, int reason, String desc) throws IOException {
        String trailer = String.format("%04d", reason) + pad(desc, 76).substring(0, 76);
        w.write(pad(tran350, 350) + trailer);
        w.newLine();
    }

    private static String pad(String s, int len) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= len) {
            return s.substring(0, len);
        }
        return s + " ".repeat(len - s.length());
    }

    private static BigDecimal nz(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }

    public record PostingResult(int processed, int rejected) {
    }
}

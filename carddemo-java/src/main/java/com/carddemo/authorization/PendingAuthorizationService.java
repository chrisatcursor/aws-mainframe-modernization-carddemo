package com.carddemo.authorization;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import com.carddemo.card.Card;
import com.carddemo.card.CardRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PendingAuthorizationService {

    private final PendingAuthSummaryRepository summaryRepository;
    private final PendingAuthDetailRepository detailRepository;
    private final FraudReportingService fraudReportingService;
    private final CardXrefRepository cardXrefRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;

    public PendingAuthorizationService(
            PendingAuthSummaryRepository summaryRepository,
            PendingAuthDetailRepository detailRepository,
            FraudReportingService fraudReportingService,
            CardXrefRepository cardXrefRepository,
            AccountRepository accountRepository,
            CardRepository cardRepository) {
        this.summaryRepository = summaryRepository;
        this.detailRepository = detailRepository;
        this.fraudReportingService = fraudReportingService;
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
        this.cardRepository = cardRepository;
    }

    public List<PendingAuthSummary> listSummaries() {
        return summaryRepository.findAll();
    }

    public Optional<PendingAuthSummary> getSummary(long acctId) {
        return summaryRepository.findById(acctId);
    }

    public List<PendingAuthDetail> listDetails(long acctId) {
        return detailRepository.findByAcctIdOrderByAuthDate9cDescAuthTime9cDesc(acctId);
    }

    public Optional<PendingAuthDetail> findDetail(long acctId, int authDate9c, int authTime9c, String cardNum) {
        return detailRepository.findByAcctIdAndAuthDate9cAndAuthTime9cAndCardNum(
                acctId, authDate9c, authTime9c, normalizeCard(cardNum));
    }

    @Transactional
    public FraudReportingService.FraudUpdateResult toggleFraud(long acctId, int authDate9c, int authTime9c, String cardNum, char action) {
        PendingAuthDetail d = detailRepository
                .findByAcctIdAndAuthDate9cAndAuthTime9cAndCardNum(acctId, authDate9c, authTime9c, normalizeCard(cardNum))
                .orElseThrow(() -> new IllegalArgumentException("Authorization not found"));
        PendingAuthSummary s = summaryRepository.findById(acctId).orElseThrow();
        FraudReportingService.FraudUpdateResult r = fraudReportingService.applyFraudAction(acctId, s.getCustId(), d, action);
        if (action == 'F' || action == 'f') {
            d.setAuthFraud("F");
        } else {
            d.setAuthFraud("R");
        }
        detailRepository.save(d);
        return r;
    }

    /**
     * COPAUA0C-style authorization: validate xref/account/card, append pending detail, approve/decline.
     */
    @Transactional
    public String processAuthorizationRequest(
            String cardNumRaw,
            BigDecimal transactionAmt,
            String merchantCategory,
            String merchantId) {
        String cardNum = normalizeCard(cardNumRaw);
        Optional<CardXref> xrefOpt = cardXrefRepository.findById(cardNum);
        if (xrefOpt.isEmpty()) {
            return "DECLINED|CARD_NOT_FOUND";
        }
        CardXref xref = xrefOpt.get();
        long acctId = xref.getAccountId();
        long custId = xref.getCustomerId();

        Optional<Account> acctOpt = accountRepository.findById(acctId);
        Optional<Card> cardOpt = cardRepository.findById(cardNum);
        if (acctOpt.isEmpty() || cardOpt.isEmpty()) {
            return "DECLINED|DATA_MISSING";
        }
        Account acct = acctOpt.get();
        Card card = cardOpt.get();
        if (!"Y".equalsIgnoreCase(trim(acct.getActiveStatus())) || !"Y".equalsIgnoreCase(trim(card.getActiveStatus()))) {
            return "DECLINED|INACTIVE";
        }

        BigDecimal avail = acct.getCreditLimit() != null && acct.getCurrentBalance() != null
                ? acct.getCreditLimit().subtract(acct.getCurrentBalance())
                : BigDecimal.ZERO;
        boolean approved = transactionAmt.compareTo(avail) <= 0;

        PendingAuthSummary summary = summaryRepository.findById(acctId).orElseGet(() -> newSummary(acctId, custId, acct));

        LocalDate today = LocalDate.now();
        int yyddd = PendingAuthPurgeSupport.currentYyddd(today);
        int authDate9c = 99999 - yyddd;
        int authTime9c = (int) (System.currentTimeMillis() % 900_000_000) + 100_000_000;

        PendingAuthDetail d = new PendingAuthDetail();
        d.setAcctId(acctId);
        d.setAuthDate9c(authDate9c);
        d.setAuthTime9c(authTime9c);
        d.setAuthOrigDate(today.format(DateTimeFormatter.ofPattern("yyMMdd")));
        d.setAuthOrigTime(LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));
        d.setCardNum(cardNum);
        d.setAuthType("AUTH");
        d.setCardExpiryDate(cardExpiryMmYy(card.getExpirationDate()));
        d.setMessageType("AUTHRQ");
        d.setMessageSource("MQ");
        d.setAuthIdCode("MQAUTH");
        d.setAuthRespCode(approved ? "00" : "05");
        d.setAuthRespReason(approved ? "0000" : "DECL");
        d.setProcessingCode("000001");
        d.setTransactionAmt(transactionAmt);
        d.setApprovedAmt(approved ? transactionAmt : BigDecimal.ZERO);
        d.setMerchantCategoryCode(trim(merchantCategory, 4));
        d.setAcqrCountryCode("840");
        d.setPosEntryMode((short) 1);
        d.setMerchantId(trim(merchantId, 15));
        d.setMerchantName("MQ Merchant");
        d.setMerchantCity("Seattle");
        d.setMerchantState("WA");
        d.setMerchantZip("98101");
        d.setTransactionId("MQ-" + System.currentTimeMillis());
        d.setMatchStatus(approved ? "P" : "D");
        d.setAuthFraud(" ");
        d.setFraudRptDate("        ");

        detailRepository.save(d);

        if (approved) {
            summary.setApprovedAuthCnt(summary.getApprovedAuthCnt() + 1);
            summary.setApprovedAuthAmt(summary.getApprovedAuthAmt().add(transactionAmt));
        } else {
            summary.setDeclinedAuthCnt(summary.getDeclinedAuthCnt() + 1);
            summary.setDeclinedAuthAmt(summary.getDeclinedAuthAmt().add(transactionAmt));
        }
        summaryRepository.save(summary);

        return approved ? "APPROVED|" + d.getTransactionId() : "DECLINED|INSUFFICIENT_FUNDS";
    }

    private PendingAuthSummary newSummary(long acctId, long custId, Account acct) {
        PendingAuthSummary s = new PendingAuthSummary();
        s.setAcctId(acctId);
        s.setCustId(custId);
        s.setAuthStatus("A");
        s.setAccountStatus1(trim(acct.getActiveStatus(), 2));
        s.setCreditLimit(acct.getCreditLimit());
        s.setCashLimit(acct.getCashCreditLimit());
        s.setCreditBalance(acct.getCurrentBalance());
        s.setCashBalance(BigDecimal.ZERO);
        s.setApprovedAuthCnt(0);
        s.setDeclinedAuthCnt(0);
        s.setApprovedAuthAmt(BigDecimal.ZERO);
        s.setDeclinedAuthAmt(BigDecimal.ZERO);
        return s;
    }

    private static String normalizeCard(String c) {
        if (c == null) {
            return "";
        }
        return c.replaceAll("\\s", "");
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim().replace("-", "");
        if (t.length() >= max) {
            return t.substring(0, max);
        }
        return t;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /** COBOL PIC X(4) card expiry; derive MMYY from ISO date when possible. */
    private static String cardExpiryMmYy(String expirationDate) {
        if (expirationDate == null || expirationDate.isBlank()) {
            return "0000";
        }
        String digits = expirationDate.replaceAll("\\D", "");
        if (digits.length() >= 8) {
            String mm = digits.substring(4, 6);
            String yy = digits.substring(2, 4);
            return mm + yy;
        }
        return trim(expirationDate, 4);
    }
}

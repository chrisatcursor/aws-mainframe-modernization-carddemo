package com.carddemo.auth.pending;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.carddemo.config.CardDemoProperties;

@Service
public class PendingAuthService {

    private final PendingAuthSummaryRepository summaryRepository;
    private final PendingAuthDetailRepository detailRepository;
    private final FraudReportService fraudReportService;
    private final CardDemoProperties properties;

    public PendingAuthService(
            PendingAuthSummaryRepository summaryRepository,
            PendingAuthDetailRepository detailRepository,
            FraudReportService fraudReportService,
            CardDemoProperties properties) {
        this.summaryRepository = summaryRepository;
        this.detailRepository = detailRepository;
        this.fraudReportService = fraudReportService;
        this.properties = properties;
    }

    public List<PendingAuthSummary> listSummaries() {
        return summaryRepository.findAllByOrderByAcctIdAsc();
    }

    public List<PendingAuthDetail> listDetails(long acctId) {
        return detailRepository.findByAcctIdOrderByAuthTsDesc(acctId);
    }

    @Transactional
    public void applyFraudToggle(long acctId, long detailId, String action) {
        PendingAuthSummary summary = summaryRepository.findById(acctId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown account"));
        PendingAuthDetail detail = detailRepository.findByIdAndAcctId(detailId, acctId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown detail"));
        fraudReportService.applyFraudAction(detail, summary.getAcctId(), summary.getCustId(), action);
        detailRepository.save(detail);
    }

    /**
     * CBPAUP0C-style purge: delete detail rows whose auth timestamp is older than {@code expiryDays}.
     * Adjusts summary counters; removes summary when no details remain or both auth counts are zero.
     */
    @Transactional
    public PurgeResult purgeExpired() {
        int expiryDays = properties.getPendingAuth().getPurgeExpiryDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(expiryDays);
        int detailsDeleted = 0;
        int summariesDeleted = 0;

        for (PendingAuthSummary s : summaryRepository.findAll()) {
            List<PendingAuthDetail> details = detailRepository.findByAcctIdOrderByAuthTsDesc(s.getAcctId());
            for (PendingAuthDetail d : details) {
                if (d.getAuthTs().isBefore(cutoff)) {
                    adjustSummaryForDelete(s, d);
                    detailRepository.delete(d);
                    detailsDeleted++;
                }
            }
            long remaining = detailRepository.findByAcctIdOrderByAuthTsDesc(s.getAcctId()).size();
            boolean countsEmpty =
                    zeroOrNull(s.getApprovedAuthCount()) && zeroOrNull(s.getDeclinedAuthCount());
            if (remaining == 0 || countsEmpty) {
                detailRepository.deleteAll(detailRepository.findByAcctIdOrderByAuthTsDesc(s.getAcctId()));
                summaryRepository.delete(s);
                summariesDeleted++;
            } else {
                summaryRepository.save(s);
            }
        }
        return new PurgeResult(detailsDeleted, summariesDeleted);
    }

    private static boolean zeroOrNull(Integer v) {
        return v == null || v <= 0;
    }

    private static void adjustSummaryForDelete(PendingAuthSummary s, PendingAuthDetail d) {
        if ("00".equals(d.getAuthRespCode())) {
            dec(s::getApprovedAuthCount, s::setApprovedAuthCount);
            subAmt(s::getApprovedAuthAmt, s::setApprovedAuthAmt, d.getApprovedAmt());
        } else {
            dec(s::getDeclinedAuthCount, s::setDeclinedAuthCount);
            subAmt(s::getDeclinedAuthAmt, s::setDeclinedAuthAmt,
                    d.getTransactionAmt() != null ? d.getTransactionAmt() : BigDecimal.ZERO);
        }
    }

    private interface IntGetter {
        Integer get();
    }

    private interface IntSetter {
        void set(Integer v);
    }

    private static void dec(IntGetter g, IntSetter s) {
        int v = g.get() == null ? 0 : g.get();
        s.set(Math.max(0, v - 1));
    }

    private interface AmtGetter {
        BigDecimal get();
    }

    private interface AmtSetter {
        void set(BigDecimal v);
    }

    private static void subAmt(AmtGetter g, AmtSetter s, BigDecimal delta) {
        BigDecimal cur = g.get() == null ? BigDecimal.ZERO : g.get();
        BigDecimal d = delta == null ? BigDecimal.ZERO : delta;
        s.set(cur.subtract(d).max(BigDecimal.ZERO));
    }

    public record PurgeResult(int detailsDeleted, int summariesDeleted) {}
}

package com.carddemo.authorization;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CBPAUP0C — purge expired pending authorization details (IMS DLET → SQL DELETE).
 */
@Service
public class PendingAuthPurgeService {

    private final PendingAuthSummaryRepository summaryRepository;
    private final PendingAuthDetailRepository detailRepository;

    public PendingAuthPurgeService(
            PendingAuthSummaryRepository summaryRepository,
            PendingAuthDetailRepository detailRepository) {
        this.summaryRepository = summaryRepository;
        this.detailRepository = detailRepository;
    }

    public record PurgeStats(int summariesRead, int detailsDeleted, int summariesDeleted) {}

    @Transactional
    public PurgeStats purgeExpired(int expiryDays, LocalDate today) {
        int summariesRead = 0;
        int detailsDeleted = 0;
        int summariesDeleted = 0;

        List<PendingAuthSummary> summaries = new ArrayList<>(summaryRepository.findAll());
        for (PendingAuthSummary s : summaries) {
            summariesRead++;
            List<PendingAuthDetail> details = detailRepository.findByAcctIdOrderByAuthDate9cDescAuthTime9cDesc(s.getAcctId());
            List<PendingAuthDetail> toRemove = new ArrayList<>();
            for (PendingAuthDetail d : details) {
                if (PendingAuthPurgeSupport.isExpired(d.getAuthDate9c(), today, expiryDays)) {
                    toRemove.add(d);
                }
            }
            for (PendingAuthDetail d : toRemove) {
                PendingAuthPurgeSupport.adjustSummaryForDeletedDetail(s, d);
                detailRepository.delete(d);
                detailsDeleted++;
            }
            if (PendingAuthPurgeSupport.shouldDeleteSummary(s)) {
                summaryRepository.delete(s);
                summariesDeleted++;
            } else if (!toRemove.isEmpty()) {
                summaryRepository.save(s);
            }
        }
        return new PurgeStats(summariesRead, detailsDeleted, summariesDeleted);
    }
}

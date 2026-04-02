package com.carddemo.batch;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.carddemo.auth.pending.PendingAuthDetail;
import com.carddemo.auth.pending.PendingAuthDetailRepository;
import com.carddemo.auth.pending.PendingAuthSummary;
import com.carddemo.auth.pending.PendingAuthSummaryRepository;
import com.carddemo.config.CardDemoProperties;

/**
 * PAUDBLOD — load pending auth from flat files (CSV staging format matching export job).
 */
@Configuration
public class PendingAuthImportJobConfig {

    public static final String JOB_NAME = "pendingAuthImportJob";

    private final PendingAuthSummaryRepository summaryRepository;
    private final PendingAuthDetailRepository detailRepository;
    private final CardDemoProperties properties;

    public PendingAuthImportJobConfig(
            PendingAuthSummaryRepository summaryRepository,
            PendingAuthDetailRepository detailRepository,
            CardDemoProperties properties) {
        this.summaryRepository = summaryRepository;
        this.detailRepository = detailRepository;
        this.properties = properties;
    }

    @Bean
    public Job pendingAuthImportJob(JobRepository jobRepository, Step pendingAuthImportStep) {
        return new JobBuilder(JOB_NAME, jobRepository).start(pendingAuthImportStep).build();
    }

    @Bean
    public Step pendingAuthImportStep(JobRepository jobRepository, PlatformTransactionManager tx) {
        return new StepBuilder("pendingAuthImportStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Path sumPath = Path.of(properties.getPendingAuth().getImportSummaryFile());
                    Path detPath = Path.of(properties.getPendingAuth().getImportDetailFile());
                    if (!Files.isRegularFile(sumPath) || !Files.isRegularFile(detPath)) {
                        throw new IllegalStateException("Import files missing: " + sumPath + ", " + detPath);
                    }
                    Map<Long, PendingAuthSummary> cache = new HashMap<>();
                    try (BufferedReader r = Files.newBufferedReader(sumPath, StandardCharsets.UTF_8)) {
                        String line = r.readLine();
                        if (line == null || !line.startsWith("acct_id")) {
                            throw new IllegalStateException("Expected header in summary file");
                        }
                        while ((line = r.readLine()) != null) {
                            if (line.isBlank()) {
                                continue;
                            }
                            String[] p = line.split(",", 5);
                            PendingAuthSummary s = new PendingAuthSummary();
                            s.setAcctId(Long.parseLong(p[0].trim()));
                            s.setCustId(Long.parseLong(p[1].trim()));
                            s.setAuthStatus(emptyToNull(p[2]));
                            s.setApprovedAuthCount(parseIntOrZero(p[3]));
                            s.setDeclinedAuthCount(parseIntOrZero(p[4]));
                            summaryRepository.save(s);
                            cache.put(s.getAcctId(), s);
                        }
                    }
                    try (BufferedReader r = Files.newBufferedReader(detPath, StandardCharsets.UTF_8)) {
                        String line = r.readLine();
                        if (line == null || !line.startsWith("acct_id")) {
                            throw new IllegalStateException("Expected header in detail file");
                        }
                        while ((line = r.readLine()) != null) {
                            if (line.isBlank()) {
                                continue;
                            }
                            String[] p = line.split(",", 5);
                            long acctId = Long.parseLong(p[0].trim());
                            if (!cache.containsKey(acctId) && summaryRepository.findById(acctId).isEmpty()) {
                                throw new IllegalStateException("No summary for acct " + acctId);
                            }
                            PendingAuthDetail d = new PendingAuthDetail();
                            d.setAcctId(acctId);
                            d.setAuthTs(LocalDateTime.parse(p[1].trim()));
                            d.setCardNum(p[2].trim());
                            d.setTransactionId(p[3].trim());
                            d.setAuthRespCode(emptyToNull(p[4]));
                            d.setApprovedAmt(BigDecimal.ZERO);
                            d.setTransactionAmt(BigDecimal.ZERO);
                            detailRepository.save(d);
                        }
                    }
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }

    private static String emptyToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static int parseIntOrZero(String s) {
        if (s == null || s.isBlank()) {
            return 0;
        }
        return Integer.parseInt(s.trim());
    }
}

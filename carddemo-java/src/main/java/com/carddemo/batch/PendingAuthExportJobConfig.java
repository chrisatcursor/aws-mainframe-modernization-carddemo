package com.carddemo.batch;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
 * DBUNLDGS / PAUDBUNL — export pending auth summary and detail to flat files (CSV staging format).
 */
@Configuration
public class PendingAuthExportJobConfig {

    public static final String JOB_NAME = "pendingAuthExportJob";

    private final PendingAuthSummaryRepository summaryRepository;
    private final PendingAuthDetailRepository detailRepository;
    private final CardDemoProperties properties;

    public PendingAuthExportJobConfig(
            PendingAuthSummaryRepository summaryRepository,
            PendingAuthDetailRepository detailRepository,
            CardDemoProperties properties) {
        this.summaryRepository = summaryRepository;
        this.detailRepository = detailRepository;
        this.properties = properties;
    }

    @Bean
    public Job pendingAuthExportJob(JobRepository jobRepository, Step pendingAuthExportStep) {
        return new JobBuilder(JOB_NAME, jobRepository).start(pendingAuthExportStep).build();
    }

    @Bean
    public Step pendingAuthExportStep(JobRepository jobRepository, PlatformTransactionManager tx) {
        return new StepBuilder("pendingAuthExportStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Path dir = Path.of(properties.getPendingAuth().getExportDir());
                    Files.createDirectories(dir);
                    Path s1 = dir.resolve("pending_auth_summary_export.csv");
                    Path s2 = dir.resolve("pending_auth_detail_export.csv");
                    try (BufferedWriter w1 = Files.newBufferedWriter(s1, StandardCharsets.UTF_8);
                            BufferedWriter w2 = Files.newBufferedWriter(s2, StandardCharsets.UTF_8)) {
                        w1.write("acct_id,cust_id,auth_status,approved_auth_count,declined_auth_count\n");
                        for (PendingAuthSummary s : summaryRepository.findAll()) {
                            w1.write(String.join(",",
                                    String.valueOf(s.getAcctId()),
                                    String.valueOf(s.getCustId()),
                                    csv(s.getAuthStatus()),
                                    String.valueOf(s.getApprovedAuthCount()),
                                    String.valueOf(s.getDeclinedAuthCount())));
                            w1.newLine();
                        }
                        w2.write("acct_id,auth_ts,card_num,transaction_id,auth_resp_code\n");
                        for (PendingAuthDetail d : detailRepository.findAll()) {
                            w2.write(String.join(",",
                                    String.valueOf(d.getAcctId()),
                                    d.getAuthTs().toString(),
                                    csv(d.getCardNum()),
                                    csv(d.getTransactionId()),
                                    csv(d.getAuthRespCode())));
                            w2.newLine();
                        }
                    }
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }

    private static String csv(String v) {
        if (v == null) {
            return "";
        }
        return v.replace(",", ";");
    }
}

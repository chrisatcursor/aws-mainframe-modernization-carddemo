package com.carddemo.batch;

import com.carddemo.authorization.PendingAuthPurgeService;
import java.time.LocalDate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PendingAuthPurgeJobConfig {

    public static final String JOB_NAME = "pendingAuthPurgeJob";

    @Bean
    public Job pendingAuthPurgeJob(JobRepository jobRepository, Step pendingAuthPurgeStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(pendingAuthPurgeStep)
                .build();
    }

    @Bean
    public Step pendingAuthPurgeStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            PendingAuthPurgeService purgeService,
            @Value("${carddemo.batch.auth-purge.expiry-days:5}") int expiryDays) {
        return new StepBuilder("pendingAuthPurgeStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    var stats = purgeService.purgeExpired(expiryDays, LocalDate.now());
                    contribution.incrementWriteCount(stats.detailsDeleted());
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}

package com.carddemo.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.carddemo.auth.pending.PendingAuthService;

/**
 * CBPAUP0C — purge expired pending authorization details (and empty summaries).
 */
@Configuration
public class PendingAuthPurgeJobConfig {

    public static final String JOB_NAME = "pendingAuthPurgeJob";

    private final PendingAuthService pendingAuthService;

    public PendingAuthPurgeJobConfig(PendingAuthService pendingAuthService) {
        this.pendingAuthService = pendingAuthService;
    }

    @Bean
    public Job pendingAuthPurgeJob(JobRepository jobRepository, Step pendingAuthPurgeStep) {
        return new JobBuilder(JOB_NAME, jobRepository).start(pendingAuthPurgeStep).build();
    }

    @Bean
    public Step pendingAuthPurgeStep(JobRepository jobRepository, PlatformTransactionManager tx) {
        return new StepBuilder("pendingAuthPurgeStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    PendingAuthService.PurgeResult r = pendingAuthService.purgeExpired();
                    chunkContext.getStepContext()
                            .getStepExecution()
                            .getExecutionContext()
                            .putInt("detailsDeleted", r.detailsDeleted());
                    chunkContext.getStepContext()
                            .getStepExecution()
                            .getExecutionContext()
                            .putInt("summariesDeleted", r.summariesDeleted());
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }
}

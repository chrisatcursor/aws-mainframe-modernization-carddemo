package com.carddemo.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ImsAuthorizationBatchJobConfig {

    public static final String LOAD_JOB = "imsAuthLoadJob";
    public static final String UNLOAD_JOB = "imsAuthUnloadJob";

    @Bean
    public Job imsAuthLoadJob(JobRepository jobRepository, Step imsAuthLoadStep) {
        return new JobBuilder(LOAD_JOB, jobRepository).start(imsAuthLoadStep).build();
    }

    @Bean
    public Step imsAuthLoadStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ImsAuthStagingService stagingService,
            @Value("${carddemo.batch.ims-auth.summary-input:classpath:batch/auth-summary-staging.txt}") Resource summaryInput,
            @Value("${carddemo.batch.ims-auth.detail-input:classpath:batch/auth-detail-staging.txt}") Resource detailInput) {
        return new StepBuilder("imsAuthLoadStep", jobRepository)
                .tasklet((c, cc) -> {
                    stagingService.loadFromStagingFiles(summaryInput, detailInput);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Job imsAuthUnloadJob(JobRepository jobRepository, Step imsAuthUnloadStep) {
        return new JobBuilder(UNLOAD_JOB, jobRepository).start(imsAuthUnloadStep).build();
    }

    @Bean
    public Step imsAuthUnloadStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ImsAuthStagingService stagingService,
            @Value("${carddemo.batch.ims-auth.summary-output}") WritableResource summaryOut,
            @Value("${carddemo.batch.ims-auth.detail-output}") WritableResource detailOut) {
        return new StepBuilder("imsAuthUnloadStep", jobRepository)
                .tasklet((c, cc) -> {
                    stagingService.unloadToFiles(summaryOut, detailOut);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}

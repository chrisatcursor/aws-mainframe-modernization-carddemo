package com.carddemo.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

    @Bean
    public Job verificationJob(JobRepository jobRepository, Step verificationStep) {
        return new JobBuilder("verificationJob", jobRepository)
            .start(verificationStep)
            .build();
    }

    @Bean
    public Step verificationStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager) {
        Tasklet noOpTasklet = (contribution, chunkContext) -> RepeatStatus.FINISHED;

        return new StepBuilder("verificationStep", jobRepository)
            .tasklet(noOpTasklet, transactionManager)
            .build();
    }
}

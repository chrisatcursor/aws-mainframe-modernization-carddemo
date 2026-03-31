package com.carddemo.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class InterestCalculationJobConfig {

    @Bean(name = "interestCalculationTasklet")
    public Tasklet interestCalculationTasklet(InterestCalculationService interestCalculationService) {
        return (contribution, chunkContext) -> {
            interestCalculationService.calculateInterest();
            return RepeatStatus.FINISHED;
        };
    }

    @Bean(name = "interestCalculationStep")
    public Step interestCalculationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("interestCalculationTasklet") Tasklet interestCalculationTasklet) {
        return new StepBuilder("interestCalculationStep", jobRepository)
                .tasklet(interestCalculationTasklet, transactionManager)
                .build();
    }

    @Bean(name = "interestCalculationJob")
    public Job interestCalculationJob(
            JobRepository jobRepository, @Qualifier("interestCalculationStep") Step interestCalculationStep) {
        return new JobBuilder("interestCalculationJob", jobRepository)
                .start(interestCalculationStep)
                .build();
    }
}

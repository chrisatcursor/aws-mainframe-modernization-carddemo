package com.carddemo.batch;

import java.nio.file.Path;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch job for CBSTM03A-style statement generation (text + HTML files).
 */
@Configuration
public class StatementGenerationJobConfig {

    public static final String JOB_NAME = "statementGenerationJob";
    public static final String STEP_NAME = "statementGenerationStep";
    public static final String PARAM_OUTPUT_DIR = "outputDir";

    public static final String TASKLET_NAME = "statementGenerationTasklet";

    @Bean(name = TASKLET_NAME)
    @StepScope
    public Tasklet statementGenerationTasklet(
            StatementGenerationService statementGenerationService,
            @Value("#{jobParameters['" + PARAM_OUTPUT_DIR + "']}") String outputDir) {
        return (contribution, chunkContext) -> {
            if (outputDir == null || outputDir.isBlank()) {
                throw new IllegalArgumentException("Job parameter '" + PARAM_OUTPUT_DIR + "' is required");
            }
            statementGenerationService.generateStatements(Path.of(outputDir));
            return RepeatStatus.FINISHED;
        };
    }

    @Bean(name = STEP_NAME)
    public Step statementGenerationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier(TASKLET_NAME) Tasklet statementGenerationTasklet) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet(statementGenerationTasklet, transactionManager)
                .build();
    }

    @Bean(name = JOB_NAME)
    public Job statementGenerationJob(
            JobRepository jobRepository, @Qualifier(STEP_NAME) Step statementGenerationStep) {
        return new JobBuilder(JOB_NAME, jobRepository).start(statementGenerationStep).build();
    }
}

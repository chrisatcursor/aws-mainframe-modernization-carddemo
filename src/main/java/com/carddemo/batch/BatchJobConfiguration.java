package com.carddemo.batch;

import com.carddemo.config.BatchProperties;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class BatchJobConfiguration {

    @Bean
    public Job transactionPostingJob(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            TransactionPostingService postingService,
            BatchProperties batchProperties) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            JobParameters jp = chunkContext.getStepContext().getStepExecution().getJobParameters();
            String name = jp.getString("inputFile");
            Path input = name != null ? Path.of(name) : Path.of(batchProperties.dalytranInputDir(), "DALYTRAN.txt");
            Path reject = Path.of(batchProperties.rejectOutputDir(), "DALYREJS.txt");
            Files.createDirectories(input.getParent());
            Files.createDirectories(reject.getParent());
            if (!Files.exists(input)) {
                Files.writeString(input, "");
            }
            TransactionPostingService.PostingResult r = postingService.postFromFile(input, reject);
            contribution.incrementWriteCount(r.processed());
            contribution.incrementFilterCount(r.rejected());
            return RepeatStatus.FINISHED;
        };
        Step step = new StepBuilder("transactionPostingStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
        return new JobBuilder("transactionPostingJob", jobRepository)
                .start(step)
                .build();
    }

    @Bean
    public Job interestCalculationJob(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            InterestCalculationService interestService) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            JobParameters jp = chunkContext.getStepContext().getStepExecution().getJobParameters();
            String parmDate = jp.getString("parmDate", "2000-01-01");
            interestService.run(parmDate);
            return RepeatStatus.FINISHED;
        };
        Step step = new StepBuilder("interestCalculationStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
        return new JobBuilder("interestCalculationJob", jobRepository)
                .start(step)
                .build();
    }

    @Bean
    public Job statementGenerationJob(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            StatementGenerationService statementService,
            BatchProperties batchProperties) {
        Tasklet tasklet = (contribution, chunkContext) -> {
            Path textDir = Path.of(batchProperties.statementTextDir());
            Path htmlDir = Path.of(batchProperties.statementHtmlDir());
            int n = statementService.generate(textDir, htmlDir);
            contribution.incrementWriteCount(n);
            return RepeatStatus.FINISHED;
        };
        Step step = new StepBuilder("statementGenerationStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
        return new JobBuilder("statementGenerationJob", jobRepository)
                .start(step)
                .build();
    }
}

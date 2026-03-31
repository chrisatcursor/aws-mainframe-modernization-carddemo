package com.carddemo.batch;

import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionRepository;
import java.util.Collections;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Batch job wiring for daily transaction posting (CBTRN02C). Excluded under the {@code test}
 * profile so existing tests that expect a single {@link org.springframework.batch.core.Job} bean
 * continue to run without change.
 */
@Configuration
@Profile("!test")
public class TransactionPostingJobConfig {

    public static final int TRANSACTION_POSTING_CHUNK_SIZE = 100;

    @Bean(name = "transactionPostingDailyItemReader")
    public ItemReader<DailyTransactionRecord> transactionPostingDailyItemReader() {
        return new ListItemReader<>(Collections.emptyList());
    }

    @Bean(name = "transactionPostingTransactionWriter")
    public RepositoryItemWriter<Transaction> transactionPostingTransactionWriter(
            TransactionRepository transactionRepository) {
        RepositoryItemWriter<Transaction> writer = new RepositoryItemWriter<>();
        writer.setRepository(transactionRepository);
        writer.setMethodName("save");
        return writer;
    }

    @Bean(name = "transactionPostingStep")
    public Step transactionPostingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<DailyTransactionRecord> transactionPostingDailyItemReader,
            TransactionPostingProcessor transactionPostingProcessor,
            RepositoryItemWriter<Transaction> transactionPostingTransactionWriter) {
        return new StepBuilder("transactionPostingStep", jobRepository)
                .<DailyTransactionRecord, Transaction>chunk(TRANSACTION_POSTING_CHUNK_SIZE, transactionManager)
                .reader(transactionPostingDailyItemReader)
                .processor(transactionPostingProcessor)
                .writer(transactionPostingTransactionWriter)
                .build();
    }

    @Bean(name = "transactionPostingJob")
    public Job transactionPostingJob(
            JobRepository jobRepository, @Qualifier("transactionPostingStep") Step transactionPostingStep) {
        return new JobBuilder("transactionPostingJob", jobRepository)
                .start(transactionPostingStep)
                .build();
    }
}

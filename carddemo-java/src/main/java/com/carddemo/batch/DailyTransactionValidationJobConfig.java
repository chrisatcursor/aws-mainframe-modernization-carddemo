package com.carddemo.batch;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch job equivalent to COBOL CBTRN01C: read DALYTRAN sequentially and validate that each
 * card number resolves through {@code card_xrefs} to an existing {@code accounts} row. Does not
 * post or persist transactions.
 */
@Configuration
public class DailyTransactionValidationJobConfig {

    private static final Logger log = LoggerFactory.getLogger(DailyTransactionValidationJobConfig.class);

    public static final int DAILY_TRANSACTION_VALIDATION_CHUNK_SIZE = 50;

    public static final String JOB_PARAMETER_INPUT_FILE = "inputFile";

    @Bean
    @StepScope
    public FlatFileItemReader<DailyTransaction> dailyTransactionValidationReader(
            @Value("#{jobParameters['inputFile']}") String inputFile) {
        return new FlatFileItemReaderBuilder<DailyTransaction>()
                .name("dailyTransactionValidationReader")
                .resource(new FileSystemResource(inputFile))
                .encoding(StandardCharsets.UTF_8.name())
                .strict(true)
                .lineMapper((line, lineNumber) -> DailyTransaction.parse(line))
                .build();
    }

    @Bean
    public ItemWriter<DailyTransaction> dailyTransactionValidationWriter() {
        return chunk -> {
            for (DailyTransaction t : chunk.getItems()) {
                log.info(
                        "Validated daily transaction: transactionId={} cardNumber={} typeCode={} amount={}",
                        t.transactionId(),
                        t.cardNumber(),
                        t.typeCode(),
                        t.amount());
            }
        };
    }

    @Bean
    public Step dailyTransactionValidationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("dailyTransactionValidationReader") FlatFileItemReader<DailyTransaction> reader,
            DailyTransactionProcessor dailyTransactionProcessor,
            ItemWriter<DailyTransaction> dailyTransactionValidationWriter) {
        return new StepBuilder("dailyTransactionValidationStep", jobRepository)
                .<DailyTransaction, DailyTransaction>chunk(
                        DAILY_TRANSACTION_VALIDATION_CHUNK_SIZE, transactionManager)
                .reader(reader)
                .processor(dailyTransactionProcessor)
                .writer(dailyTransactionValidationWriter)
                .build();
    }

    @Bean
    public Job dailyTransactionValidationJob(
            JobRepository jobRepository,
            @Qualifier("dailyTransactionValidationStep") Step dailyTransactionValidationStep) {
        return new JobBuilder("dailyTransactionValidationJob", jobRepository)
                .start(dailyTransactionValidationStep)
                .build();
    }
}

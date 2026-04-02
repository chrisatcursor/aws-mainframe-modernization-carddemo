package com.carddemo.batch;

import java.io.BufferedReader;
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

import com.carddemo.config.CardDemoProperties;
import com.carddemo.transaction.TransactionType;
import com.carddemo.transaction.TransactionTypeRepository;

/**
 * COBTUPDT — batch maintenance from sequential file (RECORDING MODE F: 1 + 2 + 50 bytes).
 */
@Configuration
public class TransactionTypeFileUpdateJobConfig {

    public static final String JOB_NAME = "transactionTypeFileUpdateJob";

    private final TransactionTypeRepository repository;
    private final CardDemoProperties properties;

    public TransactionTypeFileUpdateJobConfig(
            TransactionTypeRepository repository,
            CardDemoProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Bean
    public Job transactionTypeFileUpdateJob(JobRepository jobRepository, Step transactionTypeFileUpdateStep) {
        return new JobBuilder(JOB_NAME, jobRepository).start(transactionTypeFileUpdateStep).build();
    }

    @Bean
    public Step transactionTypeFileUpdateStep(JobRepository jobRepository, PlatformTransactionManager tx) {
        return new StepBuilder("transactionTypeFileUpdateStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Path path = Path.of(properties.getTransactionType().getBatchInputFile());
                    if (!Files.isRegularFile(path)) {
                        throw new IllegalStateException("Batch input missing: " + path);
                    }
                    try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            if (line.isBlank() || line.charAt(0) == '*') {
                                continue;
                            }
                            String rec = line.length() >= 53 ? line.substring(0, 53) : String.format("%-53s", line);
                            char op = rec.charAt(0);
                            String code = rec.substring(1, 3);
                            String desc = rec.substring(3).trim();
                            switch (op) {
                                case 'A' -> {
                                    TransactionType t = new TransactionType();
                                    t.setTrType(code);
                                    t.setDescription(desc.isEmpty() ? code : desc);
                                    repository.save(t);
                                }
                                case 'U' -> {
                                    TransactionType t = repository.findById(code)
                                            .orElseThrow(() -> new IllegalStateException("Unknown TR_TYPE " + code));
                                    t.setDescription(desc);
                                    repository.save(t);
                                }
                                case 'D' -> repository.deleteById(code);
                                default -> throw new IllegalStateException("Invalid record type: " + op);
                            }
                        }
                    }
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }
}

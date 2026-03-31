package com.carddemo.batch;

import com.carddemo.transaction.TransactionType;
import com.carddemo.transaction.TransactionTypeRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import org.springframework.transaction.PlatformTransactionManager;

/**
 * COBTUPDT — 53-byte fixed records: type (A/U/D), type code (2), description (50).
 */
@Configuration
public class TransactionTypeBatchJobConfig {

    public static final String JOB_NAME = "transactionTypeBatchJob";

    @Bean
    public Job transactionTypeBatchJob(JobRepository jobRepository, Step transactionTypeBatchStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(transactionTypeBatchStep)
                .build();
    }

    @Bean
    public Step transactionTypeBatchStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            TransactionTypeRepository typeRepository,
            @Value("${carddemo.batch.trantype.input:classpath:batch/trantype-update-sample.txt}") Resource inputFile) {
        return new StepBuilder("transactionTypeBatchStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    if (!inputFile.exists()) {
                        return RepeatStatus.FINISHED;
                    }
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(inputFile.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (line.length() < 53) {
                                line = String.format("%-53s", line);
                            }
                            char op = line.charAt(0);
                            String typeCd = line.substring(1, 3);
                            String desc = line.substring(3, 53).trim();
                            switch (op) {
                                case 'A', 'a' -> {
                                    if (!typeRepository.existsById(typeCd)) {
                                        typeRepository.save(TransactionType.of(typeCd,
                                                desc.isEmpty() ? typeCd : desc));
                                    }
                                }
                                case 'U', 'u' -> {
                                    TransactionType t = typeRepository.findById(typeCd)
                                            .orElseGet(() -> TransactionType.of(typeCd, desc));
                                    t.setTypeCode(typeCd);
                                    t.setTypeDescription(desc);
                                    typeRepository.save(t);
                                }
                                case 'D', 'd' -> typeRepository.deleteById(typeCd);
                                default -> { }
                            }
                        }
                    }
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}

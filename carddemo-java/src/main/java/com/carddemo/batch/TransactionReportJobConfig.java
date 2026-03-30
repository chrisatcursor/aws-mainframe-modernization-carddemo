package com.carddemo.batch;

import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionRepository;
import com.carddemo.transaction.TransactionTypeRepository;
import com.carddemo.transaction.TransactionCategoryRepository;
import com.carddemo.card.CardXrefRepository;
import java.util.List;
import java.util.Map;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch job for CBTRN03C: transaction detail report (TRANREPT) with date range job parameters.
 */
@Configuration
public class TransactionReportJobConfig {

    public static final int TRANSACTION_REPORT_CHUNK_SIZE = 50;

    @Bean
    @StepScope
    public RepositoryItemReader<Transaction> transactionReportReader(
            TransactionRepository transactionRepository,
            @Value("#{jobParameters['startDate']}") String startDate,
            @Value("#{jobParameters['endDate']}") String endDate) {
        RepositoryItemReader<Transaction> reader = new RepositoryItemReader<>();
        reader.setRepository(transactionRepository);
        reader.setMethodName("findForReportByProcessedDateRange");
        reader.setArguments(List.of(startDate, endDate));
        reader.setSort(
                Map.of("processedTimestamp", Sort.Direction.ASC, "transactionId", Sort.Direction.ASC));
        reader.setPageSize(TRANSACTION_REPORT_CHUNK_SIZE);
        return reader;
    }

    @Bean
    public TransactionReportProcessor transactionReportProcessor(
            TransactionTypeRepository transactionTypeRepository,
            TransactionCategoryRepository transactionCategoryRepository,
            CardXrefRepository cardXrefRepository) {
        return new TransactionReportProcessor(
                transactionTypeRepository, transactionCategoryRepository, cardXrefRepository);
    }

    @Bean
    @StepScope
    public TransactionReportWriter transactionReportWriter(
            @Value("#{jobParameters['outputFile']}") String outputFile,
            @Value("#{jobParameters['startDate']}") String startDate,
            @Value("#{jobParameters['endDate']}") String endDate,
            @Value("#{jobParameters['linesPerPage'] != null ? jobParameters['linesPerPage'] : '60'}")
                    String linesPerPageParam) {
        int linesPerPage = TransactionReportWriter.DEFAULT_LINES_PER_PAGE;
        if (linesPerPageParam != null && !linesPerPageParam.isBlank()) {
            linesPerPage = Integer.parseInt(linesPerPageParam.trim());
        }
        return new TransactionReportWriter(outputFile, startDate, endDate, linesPerPage);
    }

    @Bean
    public Step transactionReportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            RepositoryItemReader<Transaction> transactionReportReader,
            TransactionReportProcessor transactionReportProcessor,
            TransactionReportWriter transactionReportWriter) {
        return new StepBuilder("transactionReportStep", jobRepository)
                .<Transaction, TransactionReportLine>chunk(TRANSACTION_REPORT_CHUNK_SIZE, transactionManager)
                .reader(transactionReportReader)
                .processor(transactionReportProcessor)
                .writer(transactionReportWriter)
                .stream(transactionReportWriter)
                .build();
    }

    @Bean
    public Job transactionReportJob(JobRepository jobRepository, Step transactionReportStep) {
        return new JobBuilder("transactionReportJob", jobRepository)
                .start(transactionReportStep)
                .build();
    }
}

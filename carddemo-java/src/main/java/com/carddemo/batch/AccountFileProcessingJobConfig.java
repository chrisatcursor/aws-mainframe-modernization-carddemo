package com.carddemo.batch;

import com.carddemo.account.Account;
import com.carddemo.account.AccountRepository;
import java.nio.file.Paths;
import java.util.Collections;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch migration of COBOL CBACT01C: read all accounts (VSAM ACCTFILE) and write OUTFILE,
 * ARRYFILE, and VBRCFILE in the configured output directory.
 */
@Configuration
public class AccountFileProcessingJobConfig {

    public static final String JOB_NAME = "accountFileProcessingJob";
    public static final String STEP_NAME = "accountFileProcessingStep";
    public static final String PARAM_OUTPUT_DIR = "outputDir";

    public static final String OUTFILE_NAME = "OUTFILE";
    public static final String ARRYFILE_NAME = "ARRYFILE";
    public static final String VBRCFILE_NAME = "VBRCFILE";

    public static final int ACCOUNT_FILE_PROCESSING_CHUNK_SIZE = 100;

    @Bean
    public RepositoryItemReader<Account> accountProcessingReader(AccountRepository accountRepository)
            throws Exception {
        RepositoryItemReader<Account> reader = new RepositoryItemReader<>();
        reader.setRepository(accountRepository);
        reader.setMethodName("findAll");
        reader.setSort(Collections.singletonMap("acctId", Sort.Direction.ASC));
        reader.setPageSize(ACCOUNT_FILE_PROCESSING_CHUNK_SIZE);
        reader.afterPropertiesSet();
        return reader;
    }

    @Bean
    public ItemProcessor<Account, Account> accountPassThroughProcessor() {
        return item -> item;
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<Account> accountOutFileWriter(
            @Value("#{jobParameters['" + PARAM_OUTPUT_DIR + "']}") String outputDir) {
        return buildFlatFileWriter(
                "accountOutFileWriter",
                outputDir,
                OUTFILE_NAME,
                new AccountLineAggregator(AccountLineAggregator.FormatMode.FIXED_WIDTH));
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<Account> accountArryFileWriter(
            @Value("#{jobParameters['" + PARAM_OUTPUT_DIR + "']}") String outputDir) {
        return buildFlatFileWriter(
                "accountArryFileWriter",
                outputDir,
                ARRYFILE_NAME,
                new AccountLineAggregator(AccountLineAggregator.FormatMode.ARRAY));
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<Account> accountVbrcFileWriter(
            @Value("#{jobParameters['" + PARAM_OUTPUT_DIR + "']}") String outputDir) {
        return buildFlatFileWriter(
                "accountVbrcFileWriter",
                outputDir,
                VBRCFILE_NAME,
                new AccountLineAggregator(AccountLineAggregator.FormatMode.VARIABLE));
    }

    private static FlatFileItemWriter<Account> buildFlatFileWriter(
            String name, String outputDir, String fileName, AccountLineAggregator aggregator) {
        FlatFileItemWriter<Account> writer = new FlatFileItemWriter<>();
        writer.setName(name);
        writer.setResource(new FileSystemResource(Paths.get(outputDir, fileName).toString()));
        writer.setLineAggregator(aggregator);
        writer.setShouldDeleteIfExists(true);
        return writer;
    }

    @Bean
    @StepScope
    public CompositeItemWriter<Account> accountCompositeWriter(
            @Qualifier("accountOutFileWriter") FlatFileItemWriter<Account> accountOutFileWriter,
            @Qualifier("accountArryFileWriter") FlatFileItemWriter<Account> accountArryFileWriter,
            @Qualifier("accountVbrcFileWriter") FlatFileItemWriter<Account> accountVbrcFileWriter) {
        CompositeItemWriter<Account> composite = new CompositeItemWriter<>();
        composite.setDelegates(java.util.List.of(accountOutFileWriter, accountArryFileWriter, accountVbrcFileWriter));
        return composite;
    }

    @Bean
    public Step accountFileProcessingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            RepositoryItemReader<Account> accountProcessingReader,
            ItemProcessor<Account, Account> accountPassThroughProcessor,
            CompositeItemWriter<Account> accountCompositeWriter,
            @Qualifier("accountOutFileWriter") FlatFileItemWriter<Account> accountOutFileWriter,
            @Qualifier("accountArryFileWriter") FlatFileItemWriter<Account> accountArryFileWriter,
            @Qualifier("accountVbrcFileWriter") FlatFileItemWriter<Account> accountVbrcFileWriter) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<Account, Account>chunk(ACCOUNT_FILE_PROCESSING_CHUNK_SIZE, transactionManager)
                .reader(accountProcessingReader)
                .processor(accountPassThroughProcessor)
                .writer(accountCompositeWriter)
                .stream(accountOutFileWriter)
                .stream(accountArryFileWriter)
                .stream(accountVbrcFileWriter)
                .build();
    }

    @Bean
    public Job accountFileProcessingJob(
            JobRepository jobRepository, @Qualifier("accountFileProcessingStep") Step accountFileProcessingStep) {
        return new JobBuilder(JOB_NAME, jobRepository).start(accountFileProcessingStep).build();
    }
}

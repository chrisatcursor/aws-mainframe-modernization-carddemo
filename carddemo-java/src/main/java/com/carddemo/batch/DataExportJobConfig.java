package com.carddemo.batch;

import com.carddemo.account.AccountRepository;
import com.carddemo.account.CustomerRepository;
import com.carddemo.card.CardRepository;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.TransactionRepository;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch equivalent of COBOL CBEXPORT: full-database export to a single EXPFILE-style flat
 * file with type-prefixed 500-character records (CVEXPORT), plus a header line (export date) and
 * trailer line (per-type counts).
 */
@Configuration
public class DataExportJobConfig {

    public static final String JOB_NAME = "dataExportJob";
    public static final String STEP_NAME = "dataExportStep";
    public static final String PARAM_OUTPUT_FILE = "outputFile";
    public static final int CHUNK_SIZE = 50;

    @Bean
    @StepScope
    public DataExportStepContext dataExportStepContext() {
        return new DataExportStepContext();
    }

    @Bean
    @StepScope
    public ItemProcessor<Object, ExportLineEnvelope> dataExportItemProcessor(DataExportStepContext dataExportStepContext) {
        return item -> new ExportLineEnvelope(
                dataExportStepContext.nextSequence(), item, dataExportStepContext.getExportTimestamp26());
    }

    @Bean
    @StepScope
    public ItemStreamReader<Object> dataExportItemReader(
            CustomerRepository customerRepository,
            AccountRepository accountRepository,
            CardXrefRepository cardXrefRepository,
            TransactionRepository transactionRepository,
            CardRepository cardRepository) {
        return new ExportEntityItemReader(
                customerRepository,
                accountRepository,
                cardXrefRepository,
                transactionRepository,
                cardRepository);
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<ExportLineEnvelope> dataExportFileWriter(
            @Value("#{jobParameters['" + PARAM_OUTPUT_FILE + "']}") String outputFile,
            DataExportStepContext dataExportStepContext,
            ExportLineAggregator exportLineAggregator) {
        FlatFileItemWriter<ExportLineEnvelope> writer = new FlatFileItemWriter<>();
        writer.setName("dataExportFileWriter");
        writer.setResource(new FileSystemResource(outputFile));
        writer.setLineAggregator(exportLineAggregator);
        writer.setShouldDeleteIfExists(true);
        writer.setAppendAllowed(false);
        writer.setHeaderCallback(out -> out.write(dataExportStepContext.buildHeaderLine()));
        writer.setFooterCallback(out -> out.write(dataExportStepContext.buildFooterLine()));
        return writer;
    }

    @Bean(name = STEP_NAME)
    public Step dataExportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemStreamReader<Object> dataExportItemReader,
            ItemProcessor<Object, ExportLineEnvelope> dataExportItemProcessor,
            FlatFileItemWriter<ExportLineEnvelope> dataExportFileWriter,
            DataExportStepContext dataExportStepContext) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .listener((StepExecutionListener) dataExportStepContext)
                .listener((ItemWriteListener<ExportLineEnvelope>) dataExportStepContext)
                .<Object, ExportLineEnvelope>chunk(CHUNK_SIZE, transactionManager)
                .reader(dataExportItemReader)
                .processor(dataExportItemProcessor)
                .writer(dataExportFileWriter)
                .build();
    }

    @Bean(name = JOB_NAME)
    public Job dataExportJob(JobRepository jobRepository, @Qualifier(STEP_NAME) Step dataExportStep) {
        return new JobBuilder(JOB_NAME, jobRepository).start(dataExportStep).build();
    }
}

package com.carddemo.batch;

import com.carddemo.account.Account;
import com.carddemo.account.Customer;
import com.carddemo.account.CustomerRepository;
import com.carddemo.account.AccountRepository;
import com.carddemo.card.Card;
import com.carddemo.card.CardRepository;
import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import com.carddemo.transaction.Transaction;
import com.carddemo.transaction.TransactionRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.ClassifierCompositeItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DataImportJobConfig {

    public static final String JOB_NAME = "dataImportJob";
    public static final String STEP_NAME = "dataImportStep";

    public static final String CTX_COUNT_CUSTOMERS = "import.count.customers";
    public static final String CTX_COUNT_ACCOUNTS = "import.count.accounts";
    public static final String CTX_COUNT_XREFS = "import.count.xrefs";
    public static final String CTX_COUNT_TRANSACTIONS = "import.count.transactions";
    public static final String CTX_COUNT_CARDS = "import.count.cards";
    public static final String CTX_COUNT_INVALID = "import.count.invalid";

    public static final int DATA_IMPORT_CHUNK_SIZE = 50;

    private static final Logger log = LoggerFactory.getLogger(DataImportJobConfig.class);

    @Bean
    @StepScope
    public FlatFileItemReader<String> importFileReader(
            @Value("#{jobParameters['inputFile']}") String inputFile) {
        return new FlatFileItemReaderBuilder<String>()
                .name("importFileReader")
                .resource(new FileSystemResource(inputFile))
                .encoding(StandardCharsets.UTF_8.name())
                .strict(true)
                .lineMapper((line, lineNumber) -> line)
                .build();
    }

    @Bean
    public ItemProcessor<String, Object> importLineProcessor(ImportRecordMapper importRecordMapper) {
        return importRecordMapper::mapLine;
    }

    @Bean
    public RepositoryItemWriter<Customer> dataImportCustomerWriter(CustomerRepository customerRepository) {
        RepositoryItemWriter<Customer> w = new RepositoryItemWriter<>();
        w.setRepository(customerRepository);
        w.setMethodName("save");
        return w;
    }

    @Bean
    public RepositoryItemWriter<Account> dataImportAccountWriter(AccountRepository accountRepository) {
        RepositoryItemWriter<Account> w = new RepositoryItemWriter<>();
        w.setRepository(accountRepository);
        w.setMethodName("save");
        return w;
    }

    @Bean
    public RepositoryItemWriter<CardXref> dataImportXrefWriter(CardXrefRepository cardXrefRepository) {
        RepositoryItemWriter<CardXref> w = new RepositoryItemWriter<>();
        w.setRepository(cardXrefRepository);
        w.setMethodName("save");
        return w;
    }

    @Bean
    public RepositoryItemWriter<Transaction> dataImportTransactionWriter(
            TransactionRepository transactionRepository) {
        RepositoryItemWriter<Transaction> w = new RepositoryItemWriter<>();
        w.setRepository(transactionRepository);
        w.setMethodName("save");
        return w;
    }

    @Bean
    public RepositoryItemWriter<Card> dataImportCardWriter(CardRepository cardRepository) {
        RepositoryItemWriter<Card> w = new RepositoryItemWriter<>();
        w.setRepository(cardRepository);
        w.setMethodName("save");
        return w;
    }

    @Bean
    public ItemWriter<ImportRecordMapper.InvalidImportRecord> dataImportInvalidWriter() {
        return chunk -> {
            for (ImportRecordMapper.InvalidImportRecord r : chunk.getItems()) {
                log.warn(
                        "CBIMPORT skip: type={} reason={} preview={}",
                        r.typeCode(),
                        r.reason(),
                        r.linePreview());
            }
        };
    }

    @Bean
    public ImportRecordClassifier importRecordClassifier(
            RepositoryItemWriter<Customer> dataImportCustomerWriter,
            RepositoryItemWriter<Account> dataImportAccountWriter,
            RepositoryItemWriter<CardXref> dataImportXrefWriter,
            RepositoryItemWriter<Transaction> dataImportTransactionWriter,
            RepositoryItemWriter<Card> dataImportCardWriter,
            ItemWriter<ImportRecordMapper.InvalidImportRecord> dataImportInvalidWriter) {
        return new ImportRecordClassifier(
                adaptCustomer(dataImportCustomerWriter),
                adaptAccount(dataImportAccountWriter),
                adaptXref(dataImportXrefWriter),
                adaptTransaction(dataImportTransactionWriter),
                adaptCard(dataImportCardWriter),
                adaptInvalid(dataImportInvalidWriter));
    }

    @Bean
    public ClassifierCompositeItemWriter<Object> importCompositeWriter(ImportRecordClassifier importRecordClassifier) {
        ClassifierCompositeItemWriter<Object> w = new ClassifierCompositeItemWriter<>();
        w.setClassifier(importRecordClassifier);
        return w;
    }

    @Bean
    public DataImportStepMetricsListener dataImportStepMetricsListener() {
        return new DataImportStepMetricsListener();
    }

    @Bean
    public Step dataImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<String> importFileReader,
            ItemProcessor<String, Object> importLineProcessor,
            ClassifierCompositeItemWriter<Object> importCompositeWriter,
            DataImportStepMetricsListener dataImportStepMetricsListener) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<String, Object>chunk(DATA_IMPORT_CHUNK_SIZE, transactionManager)
                .reader(importFileReader)
                .processor(importLineProcessor)
                .writer(importCompositeWriter)
                .listener((StepExecutionListener) dataImportStepMetricsListener)
                .listener((ItemWriteListener<Object>) dataImportStepMetricsListener)
                .build();
    }

    @Bean(name = JOB_NAME)
    public Job dataImportJob(JobRepository jobRepository, Step dataImportStep) {
        return new JobBuilder(JOB_NAME, jobRepository).start(dataImportStep).build();
    }

    private static ItemWriter<Object> adaptCustomer(ItemWriter<Customer> delegate) {
        return chunk -> {
            List<Customer> list = chunk.getItems().stream().map(Customer.class::cast).toList();
            delegate.write(new Chunk<>(list));
        };
    }

    private static ItemWriter<Object> adaptAccount(ItemWriter<Account> delegate) {
        return chunk -> {
            List<Account> list = chunk.getItems().stream().map(Account.class::cast).toList();
            delegate.write(new Chunk<>(list));
        };
    }

    private static ItemWriter<Object> adaptXref(ItemWriter<CardXref> delegate) {
        return chunk -> {
            List<CardXref> list = chunk.getItems().stream().map(CardXref.class::cast).toList();
            delegate.write(new Chunk<>(list));
        };
    }

    private static ItemWriter<Object> adaptTransaction(ItemWriter<Transaction> delegate) {
        return chunk -> {
            List<Transaction> list = chunk.getItems().stream().map(Transaction.class::cast).toList();
            delegate.write(new Chunk<>(list));
        };
    }

    private static ItemWriter<Object> adaptCard(ItemWriter<Card> delegate) {
        return chunk -> {
            List<Card> list = chunk.getItems().stream().map(Card.class::cast).toList();
            delegate.write(new Chunk<>(list));
        };
    }

    private static ItemWriter<Object> adaptInvalid(ItemWriter<ImportRecordMapper.InvalidImportRecord> delegate) {
        return chunk -> {
            List<ImportRecordMapper.InvalidImportRecord> list =
                    chunk.getItems().stream().map(ImportRecordMapper.InvalidImportRecord.class::cast).toList();
            delegate.write(new Chunk<>(list));
        };
    }

    /** Per-step counters mirrored to {@link StepExecution#getExecutionContext()} for tests. */
    public static final class DataImportStepMetricsListener implements StepExecutionListener,
            org.springframework.batch.core.ItemWriteListener<Object> {

        private long customers;
        private long accounts;
        private long xrefs;
        private long transactions;
        private long cards;
        private long invalid;

        @Override
        public void beforeStep(StepExecution stepExecution) {
            customers = 0;
            accounts = 0;
            xrefs = 0;
            transactions = 0;
            cards = 0;
            invalid = 0;
        }

        @Override
        public void afterWrite(Chunk<?> chunk) {
            for (Object item : chunk) {
                switch (item) {
                    case Customer c -> customers++;
                    case Account a -> accounts++;
                    case CardXref x -> xrefs++;
                    case Transaction t -> transactions++;
                    case Card d -> cards++;
                    case ImportRecordMapper.InvalidImportRecord r -> invalid++;
                    default -> invalid++;
                }
            }
        }

        @Override
        public ExitStatus afterStep(StepExecution stepExecution) {
            var ctx = stepExecution.getExecutionContext();
            ctx.putLong(CTX_COUNT_CUSTOMERS, customers);
            ctx.putLong(CTX_COUNT_ACCOUNTS, accounts);
            ctx.putLong(CTX_COUNT_XREFS, xrefs);
            ctx.putLong(CTX_COUNT_TRANSACTIONS, transactions);
            ctx.putLong(CTX_COUNT_CARDS, cards);
            ctx.putLong(CTX_COUNT_INVALID, invalid);
            return ExitStatus.COMPLETED;
        }
    }
}

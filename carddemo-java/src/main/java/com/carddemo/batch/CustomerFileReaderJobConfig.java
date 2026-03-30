package com.carddemo.batch;

import com.carddemo.account.Customer;
import com.carddemo.account.CustomerRepository;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch equivalent of COBOL program CBCUS01C: sequential read of the customer file
 * (VSAM CUSTFILE / KSDS by customer id), logging each record. Data source is the {@code customers}
 * table via {@link CustomerRepository}.
 */
@Configuration
public class CustomerFileReaderJobConfig {

    public static final int CUSTOMER_FILE_READER_CHUNK_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(CustomerFileReaderJobConfig.class);

    @Bean
    public RepositoryItemReader<Customer> customerItemReader(CustomerRepository customerRepository) {
        RepositoryItemReader<Customer> reader = new RepositoryItemReader<>();
        reader.setRepository(customerRepository);
        reader.setMethodName("findAll");
        reader.setSort(Collections.singletonMap("custId", Sort.Direction.ASC));
        reader.setPageSize(CUSTOMER_FILE_READER_CHUNK_SIZE);
        return reader;
    }

    @Bean
    public ItemWriter<Customer> customerItemWriter() {
        return chunk -> {
            for (Customer customer : chunk.getItems()) {
                log.info(
                        "Customer record: custId={} firstName={} lastName={}",
                        customer.getCustId(),
                        customer.getFirstName(),
                        customer.getLastName());
            }
        };
    }

    @Bean
    public Step customerFileReaderStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            RepositoryItemReader<Customer> customerItemReader,
            ItemWriter<Customer> customerItemWriter) {
        return new StepBuilder("customerFileReaderStep", jobRepository)
                .<Customer, Customer>chunk(CUSTOMER_FILE_READER_CHUNK_SIZE, transactionManager)
                .reader(customerItemReader)
                .processor(item -> item)
                .writer(customerItemWriter)
                .build();
    }

    @Bean
    public Job customerFileReaderJob(JobRepository jobRepository, Step customerFileReaderStep) {
        return new JobBuilder("customerFileReaderJob", jobRepository)
                .start(customerFileReaderStep)
                .build();
    }
}

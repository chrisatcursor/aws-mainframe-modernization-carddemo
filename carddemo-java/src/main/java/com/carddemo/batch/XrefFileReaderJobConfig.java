package com.carddemo.batch;

import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
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
 * Spring Batch equivalent of COBOL program CBACT03C: sequential read of the card cross-reference
 * file (VSAM XREFFILE / KSDS by card number), logging each record. Data source is the {@code
 * card_xrefs} table via {@link CardXrefRepository}.
 */
@Configuration
public class XrefFileReaderJobConfig {

    public static final int XREF_FILE_READER_CHUNK_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(XrefFileReaderJobConfig.class);

    @Bean
    public RepositoryItemReader<CardXref> xrefItemReader(CardXrefRepository cardXrefRepository) {
        RepositoryItemReader<CardXref> reader = new RepositoryItemReader<>();
        reader.setRepository(cardXrefRepository);
        reader.setMethodName("findAll");
        reader.setSort(Collections.singletonMap("cardNumber", Sort.Direction.ASC));
        reader.setPageSize(XREF_FILE_READER_CHUNK_SIZE);
        return reader;
    }

    @Bean
    public ItemWriter<CardXref> xrefItemWriter() {
        return chunk -> {
            for (CardXref xref : chunk.getItems()) {
                log.info(
                        "CardXref record: cardNum={} customerId={} accountId={}",
                        xref.getCardNumber(),
                        xref.getCustomerId(),
                        xref.getAccountId());
            }
        };
    }

    @Bean
    public Step xrefFileReaderStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            RepositoryItemReader<CardXref> xrefItemReader,
            ItemWriter<CardXref> xrefItemWriter) {
        return new StepBuilder("xrefFileReaderStep", jobRepository)
                .<CardXref, CardXref>chunk(XREF_FILE_READER_CHUNK_SIZE, transactionManager)
                .reader(xrefItemReader)
                .processor(item -> item)
                .writer(xrefItemWriter)
                .build();
    }

    @Bean
    public Job xrefFileReaderJob(JobRepository jobRepository, Step xrefFileReaderStep) {
        return new JobBuilder("xrefFileReaderJob", jobRepository)
                .start(xrefFileReaderStep)
                .build();
    }
}

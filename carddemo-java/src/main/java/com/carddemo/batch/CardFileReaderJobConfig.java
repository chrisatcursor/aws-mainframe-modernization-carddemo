package com.carddemo.batch;

import com.carddemo.card.Card;
import com.carddemo.card.CardRepository;
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
 * Spring Batch equivalent of COBOL CBACT02C: sequential read of the card file (VSAM CARDFILE)
 * and display of each record. Here, cards are read from {@code cards} via JPA, ordered by card
 * number, and each row is logged.
 */
@Configuration
public class CardFileReaderJobConfig {

    private static final Logger log = LoggerFactory.getLogger(CardFileReaderJobConfig.class);

    public static final int CARD_FILE_READER_CHUNK_SIZE = 100;

    @Bean
    public Job cardFileReaderJob(JobRepository jobRepository, Step cardFileReaderStep) {
        return new JobBuilder("cardFileReaderJob", jobRepository).start(cardFileReaderStep).build();
    }

    @Bean
    public Step cardFileReaderStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            RepositoryItemReader<Card> cardItemReader,
            ItemWriter<Card> cardItemWriter) {
        return new StepBuilder("cardFileReaderStep", jobRepository)
                .<Card, Card>chunk(CARD_FILE_READER_CHUNK_SIZE, transactionManager)
                .reader(cardItemReader)
                .writer(cardItemWriter)
                .build();
    }

    @Bean
    public RepositoryItemReader<Card> cardItemReader(CardRepository cardRepository) throws Exception {
        RepositoryItemReader<Card> reader = new RepositoryItemReader<>();
        reader.setRepository(cardRepository);
        reader.setMethodName("findAll");
        reader.setSort(Collections.singletonMap("cardNumber", Sort.Direction.ASC));
        reader.setPageSize(CARD_FILE_READER_CHUNK_SIZE);
        reader.afterPropertiesSet();
        return reader;
    }

    @Bean
    public ItemWriter<Card> cardItemWriter() {
        return chunk -> {
            for (Card card : chunk) {
                log.info(
                        "CARD-RECORD cardNumber={} accountId={} cvvCode={} embossedName={} expirationDate={} activeStatus={}",
                        card.getCardNumber(),
                        card.getAccountId(),
                        card.getCvvCode(),
                        card.getEmbossedName(),
                        card.getExpirationDate(),
                        card.getActiveStatus());
            }
        };
    }
}

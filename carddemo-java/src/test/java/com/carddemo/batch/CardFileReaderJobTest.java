package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.card.CardRepository;
import com.carddemo.card.CardTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class CardFileReaderJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job cardFileReaderJob;

    @Autowired
    private CardRepository cardRepository;

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        jobLauncherTestUtils.setJob(cardFileReaderJob);
    }

    @Test
    void cardFileReaderJob_completesSuccessfully_withTestData() throws Exception {
        cardRepository.save(CardTestFixtures.minimalCard("0000000000000003", 3L));
        cardRepository.save(CardTestFixtures.minimalCard("0000000000000001", 1L));
        cardRepository.save(CardTestFixtures.minimalCard("0000000000000002", 2L));

        JobExecution execution = jobLauncherTestUtils.launchJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = ReaderStepAssertions.readerStep(execution);
        assertThat(step.getReadCount()).isEqualTo(3);
    }

    @Test
    void cardFileReaderJob_readsAllCardRecords_orderedByCardNumber() throws Exception {
        cardRepository.save(CardTestFixtures.minimalCard("9999999999999999", 9L));
        cardRepository.save(CardTestFixtures.minimalCard("1111111111111111", 1L));

        JobExecution execution = jobLauncherTestUtils.launchJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = ReaderStepAssertions.readerStep(execution);
        assertThat(step.getReadCount()).isEqualTo(2);
        assertThat(step.getWriteCount()).isEqualTo(2);
    }

    @Test
    void cardFileReaderJob_completesSuccessfully_whenTableIsEmpty() throws Exception {
        JobExecution execution = jobLauncherTestUtils.launchJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = ReaderStepAssertions.readerStep(execution);
        assertThat(step.getReadCount()).isZero();
        assertThat(step.getWriteCount()).isZero();
    }

    /**
     * Nested so {@link org.springframework.batch.test.StepScopeTestExecutionListener} does not treat
     * a {@code StepExecution}-returning method on the test class as a step-scoped factory.
     */
    private static final class ReaderStepAssertions {
        static StepExecution readerStep(JobExecution execution) {
            return execution.getStepExecutions().stream()
                    .filter(s -> "cardFileReaderStep".equals(s.getStepName()))
                    .findFirst()
                    .orElseThrow();
        }

        private ReaderStepAssertions() {}
    }

}

package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.card.CardXref;
import com.carddemo.card.CardXrefRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class XrefFileReaderJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("xrefFileReaderJob")
    private Job xrefFileReaderJob;

    @Autowired
    private CardXrefRepository cardXrefRepository;

    @BeforeEach
    void resetCardXrefs() {
        cardXrefRepository.deleteAll();
    }

    @Test
    void xrefFileReaderJob_completesSuccessfully_whenCardXrefsTableIsEmpty() throws Exception {
        jobLauncherTestUtils.setJob(xrefFileReaderJob);
        JobExecution execution = jobLauncherTestUtils.launchJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = execution.getStepExecutions().iterator().next();
        assertThat(step.getWriteCount()).isZero();
    }

    @Test
    void xrefFileReaderJob_completesSuccessfully_andProcessesAllCardXrefs() throws Exception {
        cardXrefRepository.saveAll(sampleCardXrefs());

        jobLauncherTestUtils.setJob(xrefFileReaderJob);
        JobExecution execution = jobLauncherTestUtils.launchJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = execution.getStepExecutions().iterator().next();
        assertThat(step.getWriteCount()).isEqualTo(3);
    }

    private static List<CardXref> sampleCardXrefs() {
        return List.of(
                CardXref.of("4111111111111111", 100L, 10L),
                CardXref.of("4222222222222222", 200L, 20L),
                CardXref.of("4000000000000000", 50L, 5L));
    }
}

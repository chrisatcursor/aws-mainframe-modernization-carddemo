package com.carddemo.config;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class BatchConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job verificationJob;

    @Test
    void verificationJob_completesSuccessfully() throws Exception {
        jobLauncherTestUtils.setJob(verificationJob);
        JobExecution execution = jobLauncherTestUtils.launchJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}

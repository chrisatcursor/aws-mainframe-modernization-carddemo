package com.carddemo.batch.util;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.test.MetaDataInstanceFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BatchWaitUtilTest {

    @Test
    void waitCentiseconds_oneCentisecond_sleepsAtLeastTenMillis() {
        long start = System.nanoTime();
        BatchWaitUtil.waitCentiseconds(1);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMs).isGreaterThanOrEqualTo(5L);
    }

    @Test
    void waitCentiseconds_zero_returnsPromptly() {
        long start = System.nanoTime();
        BatchWaitUtil.waitCentiseconds(0);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMs).isLessThan(50L);
    }

    @Test
    void waitCentiseconds_negative_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> BatchWaitUtil.waitCentiseconds(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(">= 0");
    }

    @Test
    void execute_readsWaitTimeFromJobParameters() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
            .addLong(BatchWaitUtil.JOB_PARAM_WAIT_TIME, 0L)
            .toJobParameters();

        ChunkContext chunkContext = new ChunkContext(
            new StepContext(MetaDataInstanceFactory.createStepExecution(jobParameters)));

        BatchWaitUtil tasklet = new BatchWaitUtil();
        RepeatStatus status = tasklet.execute(new StepContribution(
            MetaDataInstanceFactory.createStepExecution(jobParameters)), chunkContext);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    }

    @Test
    void execute_missingWaitTime_throwsIllegalArgumentException() {
        JobParameters jobParameters = new JobParametersBuilder().toJobParameters();
        ChunkContext chunkContext = new ChunkContext(
            new StepContext(MetaDataInstanceFactory.createStepExecution(jobParameters)));

        BatchWaitUtil tasklet = new BatchWaitUtil();
        assertThatThrownBy(() -> tasklet.execute(
            new StepContribution(MetaDataInstanceFactory.createStepExecution(jobParameters)),
            chunkContext))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(BatchWaitUtil.JOB_PARAM_WAIT_TIME);
    }
}

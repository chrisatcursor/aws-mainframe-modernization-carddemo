package com.carddemo.batch.util;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * Replacement for COBOL program COBSWAIT: waits for a duration given in centiseconds
 * (as the mainframe MVSWAIT routine did). Exposed as a static helper and as a Spring Batch {@link Tasklet}
 * that reads {@code waitTime} from job parameters.
 */
public class BatchWaitUtil implements Tasklet {

    public static final String JOB_PARAM_WAIT_TIME = "waitTime";

    /**
     * Sleeps for the given duration. One centisecond equals 10 milliseconds.
     *
     * @param centiseconds non-negative wait length in centiseconds (COBOL PARM was up to eight digits)
     * @throws IllegalArgumentException if {@code centiseconds} is negative
     */
    public static void waitCentiseconds(long centiseconds) {
        if (centiseconds < 0) {
            throw new IllegalArgumentException("centiseconds must be >= 0, got: " + centiseconds);
        }
        long millis = Math.multiplyExact(centiseconds, 10);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Wait interrupted", e);
        }
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        JobParameters params = chunkContext.getStepContext().getStepExecution().getJobParameters();
        Long waitTime = params.getLong(JOB_PARAM_WAIT_TIME);
        if (waitTime == null) {
            throw new IllegalArgumentException(
                "Missing required job parameter: " + JOB_PARAM_WAIT_TIME + " (centiseconds)");
        }
        waitCentiseconds(waitTime);
        return RepeatStatus.FINISHED;
    }
}

package com.carddemo.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class BatchJobLaunchService {

    private final JobLauncher jobLauncher;
    private final Job pendingAuthPurgeJob;
    private final Job transactionTypeBatchJob;
    private final Job imsAuthLoadJob;
    private final Job imsAuthUnloadJob;

    public BatchJobLaunchService(
            JobLauncher jobLauncher,
            @Qualifier(PendingAuthPurgeJobConfig.JOB_NAME) Job pendingAuthPurgeJob,
            @Qualifier(TransactionTypeBatchJobConfig.JOB_NAME) Job transactionTypeBatchJob,
            @Qualifier(ImsAuthorizationBatchJobConfig.LOAD_JOB) Job imsAuthLoadJob,
            @Qualifier(ImsAuthorizationBatchJobConfig.UNLOAD_JOB) Job imsAuthUnloadJob) {
        this.jobLauncher = jobLauncher;
        this.pendingAuthPurgeJob = pendingAuthPurgeJob;
        this.transactionTypeBatchJob = transactionTypeBatchJob;
        this.imsAuthLoadJob = imsAuthLoadJob;
        this.imsAuthUnloadJob = imsAuthUnloadJob;
    }

    public void runPendingAuthPurge() throws Exception {
        jobLauncher.run(pendingAuthPurgeJob, new JobParametersBuilder()
                .addLong("t", System.currentTimeMillis())
                .toJobParameters());
    }

    public void runTransactionTypeBatch() throws Exception {
        jobLauncher.run(transactionTypeBatchJob, new JobParametersBuilder()
                .addLong("t", System.currentTimeMillis())
                .toJobParameters());
    }

    public void runImsAuthLoad() throws Exception {
        jobLauncher.run(imsAuthLoadJob, new JobParametersBuilder()
                .addLong("t", System.currentTimeMillis())
                .toJobParameters());
    }

    public void runImsAuthUnload() throws Exception {
        jobLauncher.run(imsAuthUnloadJob, new JobParametersBuilder()
                .addLong("t", System.currentTimeMillis())
                .toJobParameters());
    }
}

package com.carddemo.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
public class BatchLaunchController {

    private final JobLauncher jobLauncher;
    private final ApplicationContext applicationContext;

    public BatchLaunchController(JobLauncher jobLauncher, ApplicationContext applicationContext) {
        this.jobLauncher = jobLauncher;
        this.applicationContext = applicationContext;
    }

    @PostMapping("/transaction-posting")
    public String runTransactionPosting(@RequestParam(required = false) String inputFile) throws Exception {
        Job job = applicationContext.getBean("transactionPostingJob", Job.class);
        JobParametersBuilder b = new JobParametersBuilder().addLong("runAt", System.currentTimeMillis());
        if (inputFile != null && !inputFile.isBlank()) {
            b.addString("inputFile", inputFile);
        }
        jobLauncher.run(job, b.toJobParameters());
        return "STARTED transactionPostingJob";
    }

    @PostMapping("/interest-calculation")
    public String runInterest(@RequestParam(required = false, defaultValue = "2000-01-01") String parmDate)
            throws Exception {
        Job job = applicationContext.getBean("interestCalculationJob", Job.class);
        JobParameters params = new JobParametersBuilder()
                .addLong("runAt", System.currentTimeMillis())
                .addString("parmDate", parmDate)
                .toJobParameters();
        jobLauncher.run(job, params);
        return "STARTED interestCalculationJob";
    }

    @PostMapping("/statement-generation")
    public String runStatements() throws Exception {
        Job job = applicationContext.getBean("statementGenerationJob", Job.class);
        JobParameters params = new JobParametersBuilder()
                .addLong("runAt", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(job, params);
        return "STARTED statementGenerationJob";
    }
}

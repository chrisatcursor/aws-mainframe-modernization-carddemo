package com.carddemo.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.carddemo.account.Customer;
import com.carddemo.account.CustomerRepository;
import com.carddemo.account.CustomerTestFactory;
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
class CustomerFileReaderJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("customerFileReaderJob")
    private Job customerFileReaderJob;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void resetCustomers() {
        customerRepository.deleteAll();
    }

    @Test
    void customerFileReaderJob_completesSuccessfully_whenCustomersTableIsEmpty() throws Exception {
        jobLauncherTestUtils.setJob(customerFileReaderJob);
        JobExecution execution = jobLauncherTestUtils.launchJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = execution.getStepExecutions().iterator().next();
        assertThat(step.getWriteCount()).isZero();
    }

    @Test
    void customerFileReaderJob_completesSuccessfully_andProcessesAllCustomers() throws Exception {
        customerRepository.saveAll(sampleCustomers());

        jobLauncherTestUtils.setJob(customerFileReaderJob);
        JobExecution execution = jobLauncherTestUtils.launchJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StepExecution step = execution.getStepExecutions().iterator().next();
        assertThat(step.getWriteCount()).isEqualTo(3);
    }

    private static List<Customer> sampleCustomers() {
        Customer c1 = CustomerTestFactory.newCustomer();
        c1.setCustId(100L);
        c1.setFirstName("Ann");
        c1.setLastName("Adams");

        Customer c2 = CustomerTestFactory.newCustomer();
        c2.setCustId(200L);
        c2.setFirstName("Bob");
        c2.setLastName("Brown");

        Customer c3 = CustomerTestFactory.newCustomer();
        c3.setCustId(50L);
        c3.setFirstName("Zoe");
        c3.setLastName("Zimmer");

        return List.of(c1, c2, c3);
    }
}

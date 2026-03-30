package com.carddemo.batch;

import com.carddemo.batch.service.StatementGenerationBatchService;
import com.carddemo.batch.service.StatementGenerationResult;
import com.carddemo.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "carddemo.bootstrap.enabled=true")
class StatementGenerationBatchServiceTest {

    @Autowired
    private StatementGenerationBatchService statementGenerationBatchService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void generateStatements_producesTextAndHtmlArtifacts() {
        if (transactionRepository.count() == 0) {
            return;
        }
        StatementGenerationResult result = statementGenerationBatchService.generateStatements(LocalDate.of(2026, 3, 30));

        assertThat(result.statementCount()).isPositive();
        assertThat(result.textStatements()).isNotEmpty();
        assertThat(result.htmlStatements()).isNotEmpty();

        String firstText = result.textStatements().getFirst();
        assertThat(firstText).contains("Statement Date: 2026-03-30");
        assertThat(firstText).contains("Total statement transactions:");

        String firstHtml = result.htmlStatements().getFirst();
        assertThat(firstHtml).contains("<html><body>");
        assertThat(firstHtml).contains("CardDemo Statement");
    }
}

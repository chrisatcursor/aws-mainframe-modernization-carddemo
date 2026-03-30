package com.carddemo.batch.controller;

import com.carddemo.batch.service.BatchPostingResult;
import com.carddemo.batch.service.InterestCalculationBatchResult;
import com.carddemo.batch.service.InterestCalculationBatchService;
import com.carddemo.batch.service.StatementGenerationBatchService;
import com.carddemo.batch.service.StatementGenerationResult;
import com.carddemo.batch.service.TransactionPostingBatchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/batch")
public class BatchJobController {

    private final TransactionPostingBatchService transactionPostingBatchService;
    private final InterestCalculationBatchService interestCalculationBatchService;
    private final StatementGenerationBatchService statementGenerationBatchService;

    public BatchJobController(TransactionPostingBatchService transactionPostingBatchService,
                              InterestCalculationBatchService interestCalculationBatchService,
                              StatementGenerationBatchService statementGenerationBatchService) {
        this.transactionPostingBatchService = transactionPostingBatchService;
        this.interestCalculationBatchService = interestCalculationBatchService;
        this.statementGenerationBatchService = statementGenerationBatchService;
    }

    @PostMapping("/posting")
    public BatchPostingResult runPosting() {
        return transactionPostingBatchService.processPostingBatch(LocalDate.now());
    }

    @PostMapping("/interest")
    public InterestCalculationBatchResult runInterest(
            @RequestParam(defaultValue = "2026-03-30")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate parmDate) {
        String parm = parmDate.toString() + "00";
        return interestCalculationBatchService.calculateInterest(parm);
    }

    @PostMapping("/statements")
    public StatementGenerationResult runStatements(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statementDate) {
        return statementGenerationBatchService.generateStatements(statementDate);
    }
}

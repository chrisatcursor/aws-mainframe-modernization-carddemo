package com.carddemo.batch.service;

public record InterestCalculationBatchResult(
        long processedBalances,
        long generatedTransactions,
        int nextTransactionSuffix
) {
    public InterestCalculationBatchResult(long processedBalances, long generatedTransactions) {
        this(processedBalances, generatedTransactions, 0);
    }
}

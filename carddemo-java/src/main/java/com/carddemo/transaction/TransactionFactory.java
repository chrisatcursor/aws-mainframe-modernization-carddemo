package com.carddemo.transaction;

/**
 * Builds {@link Transaction} instances for posting; the entity only has a protected no-arg constructor.
 */
public final class TransactionFactory {

    private TransactionFactory() {}

    public static Transaction newTransaction() {
        return new Transaction();
    }
}

package com.carddemo.batch.statement;

import com.carddemo.account.Account;
import com.carddemo.account.Customer;
import com.carddemo.card.CardXref;
import com.carddemo.transaction.Transaction;
import java.util.List;

/**
 * Assembled view of account, customer, cards, and transactions for statement generation
 * (COBOL CBSTM03B file-area aggregation).
 */
public record StatementData(
        Account account,
        Customer customer,
        List<CardXref> cards,
        List<Transaction> transactions) {}

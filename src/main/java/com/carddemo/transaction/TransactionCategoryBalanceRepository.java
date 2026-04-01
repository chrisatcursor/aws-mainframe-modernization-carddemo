package com.carddemo.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionCategoryBalanceRepository
        extends JpaRepository<TransactionCategoryBalance, TransactionCategoryBalance.TransactionCategoryBalanceKey> {
}

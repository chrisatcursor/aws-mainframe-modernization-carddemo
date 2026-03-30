package com.carddemo.transaction;

import com.carddemo.transaction.TransactionCategory.TransactionCategoryKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, TransactionCategoryKey> {
}

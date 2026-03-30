package com.carddemo.transaction.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carddemo.transaction.model.TransactionCategoryBalance;
import com.carddemo.transaction.model.TransactionCategoryBalanceId;

public interface TransactionCategoryBalanceRepository extends JpaRepository<TransactionCategoryBalance, TransactionCategoryBalanceId> {

    List<TransactionCategoryBalance> findByIdAccountIdOrderByIdTransactionTypeCodeAscIdTransactionCategoryCodeAsc(Long accountId);
}

package com.carddemo.transaction;

import com.carddemo.transaction.TransactionCategoryBalance.TransactionCategoryBalanceKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionCategoryBalanceRepository
        extends JpaRepository<TransactionCategoryBalance, TransactionCategoryBalanceKey> {

    List<TransactionCategoryBalance> findByIdAcctId(Long acctId);
}

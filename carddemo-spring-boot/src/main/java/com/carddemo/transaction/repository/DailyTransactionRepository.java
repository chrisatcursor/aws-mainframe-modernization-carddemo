package com.carddemo.transaction.repository;

import com.carddemo.transaction.model.DailyTransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyTransactionRepository extends JpaRepository<DailyTransactionRecord, String> {
}

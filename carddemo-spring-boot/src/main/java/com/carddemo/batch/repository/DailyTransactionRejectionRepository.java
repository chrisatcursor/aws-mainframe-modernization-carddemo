package com.carddemo.batch.repository;

import com.carddemo.batch.model.DailyTransactionRejection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyTransactionRejectionRepository extends JpaRepository<DailyTransactionRejection, Long> {
}

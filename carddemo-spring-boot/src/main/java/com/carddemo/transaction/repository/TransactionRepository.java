package com.carddemo.transaction.repository;

import com.carddemo.transaction.model.TransactionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionRecord, String> {

    Page<TransactionRecord> findByCardNumberOrderByProcessedTimestampDesc(String cardNumber, Pageable pageable);

    List<TransactionRecord> findByCardNumberOrderByProcessedTimestampAsc(String cardNumber);
}

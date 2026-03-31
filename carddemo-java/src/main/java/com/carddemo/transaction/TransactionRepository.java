package com.carddemo.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Page<Transaction> findByCardNumber(String cardNumber, Pageable pageable);

    Page<Transaction> findByTransactionIdGreaterThanEqual(String startId, Pageable pageable);

    /**
     * Transactions whose processed timestamp starts with a date (yyyy-MM-dd) in the inclusive range.
     * Matches CBTRN03C filtering on TRAN-PROC-TS (1:10).
     */
    @Query(
            """
            SELECT t FROM Transaction t
            WHERE t.processedTimestamp IS NOT NULL
              AND LENGTH(t.processedTimestamp) >= 10
              AND SUBSTRING(t.processedTimestamp, 1, 10) BETWEEN :startDate AND :endDate
            """)
    Page<Transaction> findForReportByProcessedDateRange(
            @Param("startDate") String startDate, @Param("endDate") String endDate, Pageable pageable);

    @Query(value = "SELECT nextval('transaction_id_seq')", nativeQuery = true)
    Long getNextTransactionId();
}

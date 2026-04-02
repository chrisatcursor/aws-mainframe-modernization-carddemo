package com.carddemo.auth.pending;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingAuthSummaryRepository extends JpaRepository<PendingAuthSummary, Long> {

    List<PendingAuthSummary> findAllByOrderByAcctIdAsc();
}

package com.carddemo.auth.pending;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingAuthDetailRepository extends JpaRepository<PendingAuthDetail, Long> {

    List<PendingAuthDetail> findByAcctIdOrderByAuthTsDesc(Long acctId);

    Optional<PendingAuthDetail> findByIdAndAcctId(Long id, Long acctId);
}

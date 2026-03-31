package com.carddemo.authorization;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingAuthSummaryRepository extends JpaRepository<PendingAuthSummary, Long> {
}

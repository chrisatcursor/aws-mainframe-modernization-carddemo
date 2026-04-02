package com.carddemo.auth.pending;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthFraudReportRepository extends JpaRepository<AuthFraudReport, AuthFraudReportId> {
}

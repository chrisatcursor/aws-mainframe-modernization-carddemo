package com.carddemo.authorization;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudReportRepository extends JpaRepository<FraudReport, FraudReport.FraudReportId> {

    boolean existsByCardNumAndAuthTs(String cardNum, Instant authTs);
}

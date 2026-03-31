package com.carddemo.authorization;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingAuthDetailRepository extends JpaRepository<PendingAuthDetail, Long> {

    List<PendingAuthDetail> findByAcctIdOrderByAuthDate9cDescAuthTime9cDesc(Long acctId);

    Optional<PendingAuthDetail> findByAcctIdAndAuthDate9cAndAuthTime9cAndCardNum(
            Long acctId, int authDate9c, int authTime9c, String cardNum);
}

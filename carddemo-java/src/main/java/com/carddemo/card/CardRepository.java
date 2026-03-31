package com.carddemo.card;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CardRepository extends JpaRepository<Card, String>, JpaSpecificationExecutor<Card> {

    List<Card> findByAccountId(Long accountId);
}

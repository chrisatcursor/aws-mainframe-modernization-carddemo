package com.carddemo.card;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, String> {

    List<Card> findByAccountId(Long accountId);
}

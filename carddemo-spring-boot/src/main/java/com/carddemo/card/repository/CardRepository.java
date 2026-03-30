package com.carddemo.card.repository;

import com.carddemo.card.model.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, String> {

    Page<Card> findByAccountId(Long accountId, Pageable pageable);
}

package com.carddemo.card;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, String> {

    List<Card> findByAccountIdOrderByCardNumberAsc(Long accountId);

    List<Card> findByCardNumberGreaterThanEqualOrderByCardNumberAsc(String cardNumber, Pageable pageable);

    Optional<Card> findByCardNumberAndAccountId(String cardNumber, Long accountId);
}

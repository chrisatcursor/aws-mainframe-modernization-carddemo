package com.carddemo.card.repository;

import com.carddemo.card.model.CardXref;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardXrefRepository extends JpaRepository<CardXref, String> {

    List<CardXref> findByAccountIdOrderByCardNumberAsc(Long accountId);

    List<CardXref> findByCustomerIdOrderByCardNumberAsc(Long customerId);

    Optional<CardXref> findByAccountIdAndCardNumber(Long accountId, String cardNumber);
}

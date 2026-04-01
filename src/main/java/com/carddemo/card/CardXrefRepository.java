package com.carddemo.card;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardXrefRepository extends JpaRepository<CardXref, String> {

    Optional<CardXref> findByAccountId(Long accountId);

    List<CardXref> findAllByOrderByCardNumberAsc();
}

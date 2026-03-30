package com.carddemo.card;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardXrefRepository extends JpaRepository<CardXref, String> {

    List<CardXref> findByAccountId(Long accountId);

    List<CardXref> findByCustomerId(Long customerId);
}

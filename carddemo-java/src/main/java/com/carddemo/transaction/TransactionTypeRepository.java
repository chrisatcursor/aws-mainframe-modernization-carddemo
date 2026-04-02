package com.carddemo.transaction;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionTypeRepository extends JpaRepository<TransactionType, String> {

    List<TransactionType> findAllByOrderByTrTypeAsc();
}

package com.carddemo.transaction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.carddemo.transaction.model.DisclosureGroup;
import com.carddemo.transaction.model.DisclosureGroupId;

public interface DisclosureGroupRepository extends JpaRepository<DisclosureGroup, DisclosureGroupId> {

    Optional<DisclosureGroup> findByIdAccountGroupIdAndIdTransactionTypeCodeAndIdTransactionCategoryCode(
            String accountGroupId,
            String transactionTypeCode,
            Integer transactionCategoryCode
    );
}

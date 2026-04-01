package com.carddemo.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DisclosureGroupRepository
        extends JpaRepository<DisclosureGroup, DisclosureGroup.DisclosureGroupKey> {

    Optional<DisclosureGroup> findById_AccountGroupIdAndId_TranTypeCodeAndId_TranCategoryCode(
            String accountGroupId, String tranTypeCode, Integer tranCategoryCode);
}

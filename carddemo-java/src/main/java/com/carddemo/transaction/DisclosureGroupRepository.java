package com.carddemo.transaction;

import com.carddemo.transaction.DisclosureGroup.DisclosureGroupKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisclosureGroupRepository extends JpaRepository<DisclosureGroup, DisclosureGroupKey> {
}

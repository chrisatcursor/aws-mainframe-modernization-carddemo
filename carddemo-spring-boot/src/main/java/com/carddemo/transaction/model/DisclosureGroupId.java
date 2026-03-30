package com.carddemo.transaction.model;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record DisclosureGroupId(
        @Column(name = "account_group_id", nullable = false, length = 10)
        String accountGroupId,
        @Column(name = "transaction_type_code", nullable = false, length = 2)
        String transactionTypeCode,
        @Column(name = "transaction_category_code", nullable = false)
        Integer transactionCategoryCode
) implements Serializable {
}

package com.carddemo.transaction;

import java.math.BigDecimal;

/** Same-package access to {@link DisclosureGroup} for tests (protected no-arg constructor). */
public final class DisclosureGroupTestFactory {

    private DisclosureGroupTestFactory() {}

    public static DisclosureGroup newDisclosureGroup(
            String groupId, String typeCode, int categoryCode, BigDecimal interestRate) {
        DisclosureGroup d = new DisclosureGroup();
        d.setId(new DisclosureGroup.DisclosureGroupKey(groupId, typeCode, categoryCode));
        d.setInterestRate(interestRate);
        return d;
    }
}

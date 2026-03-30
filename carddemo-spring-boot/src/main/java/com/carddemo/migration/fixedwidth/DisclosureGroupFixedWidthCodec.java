package com.carddemo.migration.fixedwidth;

import com.carddemo.transaction.model.DisclosureGroup;
import com.carddemo.transaction.model.DisclosureGroupId;

public final class DisclosureGroupFixedWidthCodec {

    private DisclosureGroupFixedWidthCodec() {
    }

    public static DisclosureGroup decode(String line) {
        String record = FixedWidthSupport.normalize(line, 50);
        DisclosureGroup row = new DisclosureGroup();
        String accountGroupId = FieldParsers.text(record, 0, 10);
        String typeCode = FieldParsers.text(record, 10, 12);
        Integer categoryCode = FieldParsers.numericInt(record, 12, 16);
        row.setId(new DisclosureGroupId(accountGroupId, typeCode, categoryCode));
        row.setInterestRate(FieldParsers.signedDecimalOverpunch(record, 16, 22, 2));
        return row;
    }
}

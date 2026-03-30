package com.carddemo.migration.fixedwidth;

import com.carddemo.account.model.Account;

public class AccountFixedWidthCodec {

    private AccountFixedWidthCodec() {
    }

    public static Account decode(String line) {
        String normalized = FixedWidthSupport.requireLength(line, 300, "account");
        Account record = new Account();
        record.setAccountId(FieldParsers.numericLong(normalized, 0, 11));
        record.setActiveStatus(FieldParsers.text(normalized, 11, 12));
        record.setCurrentBalance(FieldParsers.signedDecimalOverpunch(normalized, 12, 24, 2));
        record.setCreditLimit(FieldParsers.signedDecimalOverpunch(normalized, 24, 36, 2));
        record.setCashCreditLimit(FieldParsers.signedDecimalOverpunch(normalized, 36, 48, 2));
        record.setOpenDate(FieldParsers.date(normalized, 48, 58));
        record.setExpirationDate(FieldParsers.date(normalized, 58, 68));
        record.setReissueDate(FieldParsers.date(normalized, 68, 78));
        record.setCurrentCycleCredit(FieldParsers.signedDecimalOverpunch(normalized, 78, 90, 2));
        record.setCurrentCycleDebit(FieldParsers.signedDecimalOverpunch(normalized, 90, 102, 2));
        record.setAddressZip(FieldParsers.text(normalized, 102, 112));
        record.setGroupId(FieldParsers.text(normalized, 112, 122));
        return record;
    }
}

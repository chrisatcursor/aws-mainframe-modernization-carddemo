package com.carddemo.migration.fixedwidth;

import com.carddemo.transaction.model.TransactionCategoryBalance;
import com.carddemo.transaction.model.TransactionCategoryBalanceId;

public class TransactionCategoryBalanceFixedWidthCodec {

    private TransactionCategoryBalanceFixedWidthCodec() {
    }

    public static TransactionCategoryBalance decode(String line) {
        String record = FixedWidthSupport.normalize(line, 50);
        TransactionCategoryBalance entity = new TransactionCategoryBalance();
        entity.setId(new TransactionCategoryBalanceId(
                FieldParsers.numericLong(record, 0, 11),
                FieldParsers.text(record, 11, 13),
                FieldParsers.numericInt(record, 13, 17)
        ));
        entity.setBalance(FieldParsers.signedDecimalOverpunch(record, 17, 28, 2));
        return entity;
    }
}

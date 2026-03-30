package com.carddemo.migration.fixedwidth;

import com.carddemo.card.model.CardXref;
public class CardXrefFixedWidthCodec {

    private CardXrefFixedWidthCodec() {
    }

    public static CardXref decode(String line) {
        String normalized = FixedWidthSupport.requireLength(line, 36, "cardxref");
        CardXref xref = new CardXref();
        xref.setCardNumber(FieldParsers.text(normalized, 0, 16));
        xref.setCustomerId(FieldParsers.numericLong(normalized, 16, 25));
        xref.setAccountId(FieldParsers.numericLong(normalized, 25, 36));
        return xref;
    }
}

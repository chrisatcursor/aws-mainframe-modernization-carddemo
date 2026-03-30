package com.carddemo.migration.fixedwidth;

import com.carddemo.card.model.Card;

public final class CardFixedWidthCodec {

    private CardFixedWidthCodec() {
    }

    public static Card decode(String line) {
        String value = FixedWidthSupport.requireLength(line, 150);
        Card card = new Card();
        card.setCardNumber(FieldParsers.text(value, 0, 16));
        card.setAccountId(FieldParsers.numericLong(value, 16, 27));
        card.setCvv(FieldParsers.numericInt(value, 27, 30));
        card.setEmbossedName(FieldParsers.text(value, 30, 80));
        card.setExpirationDate(FieldParsers.date(value, 80, 90));
        card.setActiveStatus(FieldParsers.text(value, 90, 91));
        return card;
    }
}

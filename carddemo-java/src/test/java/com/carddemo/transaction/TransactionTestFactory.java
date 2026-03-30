package com.carddemo.transaction;

import java.math.BigDecimal;

/** Same-package access to {@link Transaction} for tests (protected no-arg constructor). */
public final class TransactionTestFactory {

    private TransactionTestFactory() {}

    public static Transaction minimal(String tranId, String cardNum) {
        Transaction t = new Transaction();
        t.setTransactionId(tranId);
        t.setCardNumber(cardNum);
        t.setTypeCode("01");
        t.setCategoryCode(1);
        t.setSource("WEB");
        t.setDescription("Test");
        t.setAmount(new BigDecimal("10.00"));
        t.setMerchantId(1L);
        t.setMerchantName("Merchant");
        t.setMerchantCity("City");
        t.setMerchantZip("12345");
        t.setOriginTimestamp("2025-01-01 10:00:00.000000");
        t.setProcessedTimestamp("2025-01-01 10:00:01.000000");
        return t;
    }
}

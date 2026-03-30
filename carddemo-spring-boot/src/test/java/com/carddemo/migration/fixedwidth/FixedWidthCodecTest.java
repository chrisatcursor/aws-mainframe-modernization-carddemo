package com.carddemo.migration.fixedwidth;

import com.carddemo.account.model.Account;
import com.carddemo.account.model.Customer;
import com.carddemo.card.model.Card;
import com.carddemo.card.model.CardXref;
import com.carddemo.transaction.model.DailyTransactionRecord;
import com.carddemo.transaction.model.DisclosureGroup;
import com.carddemo.transaction.model.TransactionCategoryBalance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedWidthCodecTest {

    @Test
    void shouldDecodeAccountRecord() {
        String line = "00000000001Y00000001940{00000020200{00000010200{2014-11-202025-05-202025-05-2000000000000{00000000000{A000000000                                                                                                                                                                                            ";
        Account account = AccountFixedWidthCodec.decode(line);

        assertEquals(1L, account.getAccountId());
        assertEquals("Y", account.getActiveStatus());
        assertEquals(new BigDecimal("194.00"), account.getCurrentBalance());
        assertEquals(new BigDecimal("2020.00"), account.getCreditLimit());
        assertEquals(new BigDecimal("1020.00"), account.getCashCreditLimit());
        assertEquals("A000000000", account.getAddressZip());
        assertEquals("", account.getGroupId());
    }

    @Test
    void shouldDecodeCardRecord() {
        String line = "050002445376574000000000050747Aniya Von                                         2023-03-09Y                                                           ";
        Card card = CardFixedWidthCodec.decode(line);

        assertEquals("0500024453765740", card.getCardNumber());
        assertEquals(50L, card.getAccountId());
        assertEquals(747, card.getCvv());
        assertEquals("Aniya Von", card.getEmbossedName());
        assertEquals("Y", card.getActiveStatus());
    }

    @Test
    void shouldDecodeCardXrefRecord() {
        String line = "050002445376574000000005000000000050";
        CardXref xref = CardXrefFixedWidthCodec.decode(line);

        assertEquals("0500024453765740", xref.getCardNumber());
        assertEquals(50L, xref.getCustomerId());
        assertEquals(50L, xref.getAccountId());
    }

    @Test
    void shouldDecodeCustomerRecord() {
        String line = "000000001Immanuel                 Madeline                 Kessler                  618 Deshaun Route                                 Apt. 802                                          Altenwerthshire                                   NCUSA12546     (908)119-8310  (373)693-8684  020973888000000000000493684371961-06-080053581756Y274                                                                                                                                                                        ";
        Customer customer = CustomerFixedWidthCodec.decode(line);

        assertEquals(1L, customer.getCustomerId());
        assertEquals("Immanuel", customer.getFirstName());
        assertEquals("Kessler", customer.getLastName());
        assertEquals("NC", customer.getStateCode());
        assertEquals("USA", customer.getCountryCode());
        assertEquals("020973888", customer.getSsn());
        assertEquals(274, customer.getFicoScore());
    }

    @Test
    void shouldDecodeDailyTransactionRecord() {
        String line = "0000000000683580010001POS TERM  Purchase at Abshire-Lowe                                                                            0000005047G800000000Abshire-Lowe                                      North Enoshaven                                   72112     48594526128770652022-06-10 19:27:53.000000                                              ";
        DailyTransactionRecord record = DailyTransactionFixedWidthCodec.decode(line);

        assertEquals("0000000000683580", record.getTransactionId());
        assertEquals("01", record.getTransactionTypeCode());
        assertEquals(1, record.getTransactionCategoryCode());
        assertEquals(new BigDecimal("504.77"), record.getAmount());
        assertEquals("4859452612877065", record.getCardNumber());
    }

    @Test
    void shouldDecodeTransactionCategoryBalanceRecord() {
        String line = "000000000010100010000000000{0000000000000000000000";
        TransactionCategoryBalance balance = TransactionCategoryBalanceFixedWidthCodec.decode(line);

        assertEquals(1L, balance.getId().getAccountId());
        assertEquals("01", balance.getId().getTransactionTypeCode());
        assertEquals(1, balance.getId().getTransactionCategoryCode());
        assertEquals(new BigDecimal("0.00"), balance.getBalance());
    }

    @Test
    void shouldDecodeDisclosureGroupRecord() {
        String line = "A00000000001000100150{0000000000000000000000000000";
        DisclosureGroup group = DisclosureGroupFixedWidthCodec.decode(line);

        assertEquals("A000000000", group.getId().accountGroupId());
        assertEquals("01", group.getId().transactionTypeCode());
        assertEquals(1, group.getId().transactionCategoryCode());
        assertEquals(new BigDecimal("15.00"), group.getInterestRate());
    }
}

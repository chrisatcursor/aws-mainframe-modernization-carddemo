package com.carddemo.batch;

import com.carddemo.account.Account;
import com.carddemo.account.Customer;
import com.carddemo.card.Card;
import com.carddemo.card.CardXref;
import com.carddemo.common.StringPaddingUtil;
import com.carddemo.transaction.Transaction;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Parses CardDemo branch-migration export lines ({@code CVEXPORT} / {@code CBIMPORT}).
 * <p>
 * COBOL uses a 500-byte record: 1-byte type, 26-byte timestamp, {@code PIC 9(9) COMP} sequence (4
 * bytes), 4-byte branch, 5-byte region, 460-byte payload. The Java stack uses a <strong>text</strong>
 * encoding of the same layout: sequence is 4 ASCII digits in the former binary slot so lines are
 * portable UTF-8 text of length 500.
 */
@Component
public class ImportRecordMapper {

    /** Total {@code EXPORT-INPUT-RECORD} length (CBIMPORT). */
    public static final int RECORD_LENGTH = 500;

    public static final int OFFSET_TYPE = 0;
    public static final int LEN_TYPE = 1;
    public static final int OFFSET_TIMESTAMP = 1;
    public static final int LEN_TIMESTAMP = 26;
    public static final int OFFSET_SEQUENCE = 27;
    public static final int LEN_SEQUENCE = 4;
    public static final int OFFSET_BRANCH = 31;
    public static final int LEN_BRANCH = 4;
    public static final int OFFSET_REGION = 35;
    public static final int LEN_REGION = 5;
    public static final int OFFSET_DATA = 40;
    public static final int LEN_DATA = 460;

    public static final char TYPE_CUSTOMER = 'C';
    public static final char TYPE_ACCOUNT = 'A';
    public static final char TYPE_XREF = 'X';
    public static final char TYPE_TRANSACTION = 'T';
    public static final char TYPE_CARD = 'D';

    public record InvalidImportRecord(String linePreview, String typeCode, String reason) {}

    public String getRecordType(String line) {
        if (line == null || line.isEmpty()) {
            return " ";
        }
        return normalizeLine(line).substring(OFFSET_TYPE, OFFSET_TYPE + LEN_TYPE);
    }

    /**
     * @return a domain entity ({@link Customer}, {@link Account}, {@link Card}, {@link CardXref},
     *     {@link Transaction}) or {@link InvalidImportRecord}
     */
    public Object mapLine(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return new InvalidImportRecord("", " ", "Blank line");
        }
        String line = normalizeLine(rawLine);
        if (line.length() != RECORD_LENGTH) {
            return new InvalidImportRecord(preview(rawLine), "?", "Line length is not " + RECORD_LENGTH);
        }
        String type = line.substring(OFFSET_TYPE, OFFSET_TYPE + LEN_TYPE);
        String data = line.substring(OFFSET_DATA, OFFSET_DATA + LEN_DATA);
        return switch (type) {
            case "C" -> parseCustomer(data, line);
            case "A" -> parseAccount(data, line);
            case "X" -> parseXref(data, line);
            case "T" -> parseTransaction(data, line);
            case "D" -> parseCard(data, line);
            default -> new InvalidImportRecord(preview(line), type, "Unknown record type");
        };
    }

    /**
     * Builds one 500-character export line (text encoding). {@code dataArea} is padded or truncated
     * to 460 characters.
     */
    public static String formatExportLine(
            char recordType,
            String timestamp,
            long sequence,
            String branchId,
            String regionCode,
            String dataArea) {
        String head =
                String.valueOf(recordType)
                        + StringPaddingUtil.rightPad(timestamp, LEN_TIMESTAMP)
                        + StringPaddingUtil.zeroPad(sequence, LEN_SEQUENCE)
                        + StringPaddingUtil.rightPad(branchId, LEN_BRANCH)
                        + StringPaddingUtil.rightPad(regionCode, LEN_REGION);
        String data = StringPaddingUtil.rightPad(dataArea, LEN_DATA);
        if (data.length() > LEN_DATA) {
            data = data.substring(0, LEN_DATA);
        }
        String line = head + data;
        if (line.length() != RECORD_LENGTH) {
            throw new IllegalStateException("Built line length " + line.length());
        }
        return line;
    }

    public static String buildCustomerDataArea(
            long custId,
            String firstName,
            String middleName,
            String lastName,
            String addr1,
            String addr2,
            String addr3,
            String state,
            String country,
            String zip,
            String phone1,
            String phone2,
            long ssn,
            String govtId,
            String dob,
            String eftId,
            String priInd,
            int fico) {
        StringBuilder b = new StringBuilder(LEN_DATA);
        b.append(StringPaddingUtil.zeroPad(custId, 10));
        b.append(StringPaddingUtil.rightPad(firstName, 25));
        b.append(StringPaddingUtil.rightPad(middleName, 25));
        b.append(StringPaddingUtil.rightPad(lastName, 25));
        b.append(StringPaddingUtil.rightPad(addr1, 50));
        b.append(StringPaddingUtil.rightPad(addr2, 50));
        b.append(StringPaddingUtil.rightPad(addr3, 50));
        b.append(StringPaddingUtil.rightPad(state, 2));
        b.append(StringPaddingUtil.rightPad(country, 3));
        b.append(StringPaddingUtil.rightPad(zip, 10));
        b.append(StringPaddingUtil.rightPad(phone1, 15));
        b.append(StringPaddingUtil.rightPad(phone2, 15));
        b.append(StringPaddingUtil.zeroPad(ssn, 9));
        b.append(StringPaddingUtil.rightPad(govtId, 20));
        b.append(StringPaddingUtil.rightPad(dob, 10));
        b.append(StringPaddingUtil.rightPad(eftId, 10));
        b.append(StringPaddingUtil.rightPad(priInd, 1));
        b.append(StringPaddingUtil.zeroPad(fico, 3));
        while (b.length() < LEN_DATA) {
            b.append(' ');
        }
        return b.substring(0, LEN_DATA);
    }

    public static String buildAccountDataArea(
            long acctId,
            String active,
            BigDecimal currBal,
            BigDecimal creditLimit,
            BigDecimal cashCreditLimit,
            String openDate,
            String expDate,
            String reissueDate,
            BigDecimal cycCredit,
            BigDecimal cycDebit,
            String addrZip,
            String groupId) {
        StringBuilder b = new StringBuilder(LEN_DATA);
        b.append(StringPaddingUtil.zeroPad(acctId, 11));
        b.append(StringPaddingUtil.rightPad(active, 1));
        b.append(AccountLineAggregator.formatImpliedDecimalMoney(currBal));
        b.append(AccountLineAggregator.formatImpliedDecimalMoney(creditLimit));
        b.append(AccountLineAggregator.formatImpliedDecimalMoney(cashCreditLimit));
        b.append(StringPaddingUtil.rightPad(openDate, 10));
        b.append(StringPaddingUtil.rightPad(expDate, 10));
        b.append(StringPaddingUtil.rightPad(reissueDate, 10));
        b.append(AccountLineAggregator.formatImpliedDecimalMoney(cycCredit));
        b.append(AccountLineAggregator.formatImpliedDecimalMoney(cycDebit));
        b.append(StringPaddingUtil.rightPad(addrZip, 10));
        b.append(StringPaddingUtil.rightPad(groupId, 10));
        while (b.length() < LEN_DATA) {
            b.append(' ');
        }
        return b.substring(0, LEN_DATA);
    }

    public static String buildXrefDataArea(String cardNum, long custId, long acctId) {
        StringBuilder b = new StringBuilder(LEN_DATA);
        b.append(StringPaddingUtil.rightPad(cardNum, 16));
        b.append(StringPaddingUtil.zeroPad(custId, 9));
        b.append(StringPaddingUtil.zeroPad(acctId, 11));
        while (b.length() < LEN_DATA) {
            b.append(' ');
        }
        return b.substring(0, LEN_DATA);
    }

    public static String buildTransactionDataArea(
            String tranId,
            String typeCd,
            int catCd,
            String source,
            String desc,
            BigDecimal amount,
            long merchantId,
            String merchantName,
            String merchantCity,
            String merchantZip,
            String cardNum,
            String origTs,
            String procTs) {
        StringBuilder b = new StringBuilder(LEN_DATA);
        b.append(StringPaddingUtil.rightPad(tranId, 16));
        b.append(StringPaddingUtil.rightPad(typeCd, 2));
        b.append(StringPaddingUtil.zeroPad(catCd, 4));
        b.append(StringPaddingUtil.rightPad(source, 10));
        b.append(StringPaddingUtil.rightPad(desc, 100));
        b.append(AccountLineAggregator.formatImpliedDecimalMoney(amount));
        b.append(StringPaddingUtil.zeroPad(merchantId, 9));
        b.append(StringPaddingUtil.rightPad(merchantName, 50));
        b.append(StringPaddingUtil.rightPad(merchantCity, 50));
        b.append(StringPaddingUtil.rightPad(merchantZip, 10));
        b.append(StringPaddingUtil.rightPad(cardNum, 16));
        b.append(StringPaddingUtil.rightPad(origTs, 26));
        b.append(StringPaddingUtil.rightPad(procTs, 26));
        while (b.length() < LEN_DATA) {
            b.append(' ');
        }
        return b.substring(0, LEN_DATA);
    }

    public static String buildCardDataArea(
            String cardNum,
            long acctId,
            int cvv,
            String embossed,
            String expDate,
            String active) {
        StringBuilder b = new StringBuilder(LEN_DATA);
        b.append(StringPaddingUtil.rightPad(cardNum, 16));
        b.append(StringPaddingUtil.zeroPad(acctId, 11));
        b.append(StringPaddingUtil.zeroPad(cvv, 3));
        b.append(StringPaddingUtil.rightPad(embossed, 50));
        b.append(StringPaddingUtil.rightPad(expDate, 10));
        b.append(StringPaddingUtil.rightPad(active, 1));
        while (b.length() < LEN_DATA) {
            b.append(' ');
        }
        return b.substring(0, LEN_DATA);
    }

    private static String normalizeLine(String raw) {
        if (raw.length() >= RECORD_LENGTH) {
            return raw.substring(0, RECORD_LENGTH);
        }
        return StringPaddingUtil.rightPad(raw, RECORD_LENGTH);
    }

    private static String preview(String line) {
        int n = Math.min(80, line.length());
        return line.substring(0, n);
    }

    private static Object parseCustomer(String data, String fullLine) {
        try {
            if (data.length() < LEN_DATA) {
                return new InvalidImportRecord(preview(fullLine), "C", "Customer payload truncated");
            }
            int p = 0;
            long custId = Long.parseLong(data.substring(p, p + 10).trim());
            p += 10;
            Customer c = Customer.forImport();
            c.setCustId(custId);
            c.setFirstName(trimField(data.substring(p, p + 25)));
            p += 25;
            c.setMiddleName(trimField(data.substring(p, p + 25)));
            p += 25;
            c.setLastName(trimField(data.substring(p, p + 25)));
            p += 25;
            c.setAddressLine1(trimField(data.substring(p, p + 50)));
            p += 50;
            c.setAddressLine2(trimField(data.substring(p, p + 50)));
            p += 50;
            c.setAddressLine3(trimField(data.substring(p, p + 50)));
            p += 50;
            c.setAddressStateCode(trimField(data.substring(p, p + 2)));
            p += 2;
            c.setAddressCountryCode(trimField(data.substring(p, p + 3)));
            p += 3;
            c.setAddressZip(trimField(data.substring(p, p + 10)));
            p += 10;
            c.setPhoneNumber1(trimField(data.substring(p, p + 15)));
            p += 15;
            c.setPhoneNumber2(trimField(data.substring(p, p + 15)));
            p += 15;
            c.setSsn(Long.parseLong(data.substring(p, p + 9).trim()));
            p += 9;
            c.setGovtIssuedId(trimField(data.substring(p, p + 20)));
            p += 20;
            c.setDateOfBirth(trimField(data.substring(p, p + 10)));
            p += 10;
            c.setEftAccountId(trimField(data.substring(p, p + 10)));
            p += 10;
            c.setPrimaryCardHolderIndicator(trimField(data.substring(p, p + 1)));
            p += 1;
            c.setFicoCreditScore(Integer.parseInt(data.substring(p, p + 3).trim()));
            return c;
        } catch (RuntimeException ex) {
            return new InvalidImportRecord(preview(fullLine), "C", "Customer parse error: " + ex.getMessage());
        }
    }

    private static Object parseAccount(String data, String fullLine) {
        try {
            if (data.length() < 122) {
                return new InvalidImportRecord(preview(fullLine), "A", "Account payload truncated");
            }
            int p = 0;
            long acctId = Long.parseLong(data.substring(p, p + 11).trim());
            p += 11;
            Account a = new Account();
            a.setAcctId(acctId);
            a.setActiveStatus(trimField(data.substring(p, p + 1)));
            p += 1;
            a.setCurrentBalance(parseImpliedDecimal12(data.substring(p, p + 12)));
            p += 12;
            a.setCreditLimit(parseImpliedDecimal12(data.substring(p, p + 12)));
            p += 12;
            a.setCashCreditLimit(parseImpliedDecimal12(data.substring(p, p + 12)));
            p += 12;
            a.setOpenDate(trimField(data.substring(p, p + 10)));
            p += 10;
            a.setExpirationDate(trimField(data.substring(p, p + 10)));
            p += 10;
            a.setReissueDate(trimField(data.substring(p, p + 10)));
            p += 10;
            a.setCurrentCycleCredit(parseImpliedDecimal12(data.substring(p, p + 12)));
            p += 12;
            a.setCurrentCycleDebit(parseImpliedDecimal12(data.substring(p, p + 12)));
            p += 12;
            a.setAddressZip(trimField(data.substring(p, p + 10)));
            p += 10;
            a.setGroupId(trimField(data.substring(p, p + 10)));
            return a;
        } catch (RuntimeException ex) {
            return new InvalidImportRecord(preview(fullLine), "A", "Account parse error: " + ex.getMessage());
        }
    }

    private static Object parseXref(String data, String fullLine) {
        try {
            if (data.length() < 36) {
                return new InvalidImportRecord(preview(fullLine), "X", "Xref payload truncated");
            }
            String cardNum = trimField(data.substring(0, 16));
            long custId = Long.parseLong(data.substring(16, 25).trim());
            long acctId = Long.parseLong(data.substring(25, 36).trim());
            return CardXref.of(cardNum, custId, acctId);
        } catch (RuntimeException ex) {
            return new InvalidImportRecord(preview(fullLine), "X", "Xref parse error: " + ex.getMessage());
        }
    }

    private static Object parseTransaction(String data, String fullLine) {
        try {
            if (data.length() < 331) {
                return new InvalidImportRecord(preview(fullLine), "T", "Transaction payload truncated");
            }
            int p = 0;
            Transaction t = Transaction.forImport();
            t.setTransactionId(trimField(data.substring(p, p + 16)));
            p += 16;
            t.setTypeCode(trimField(data.substring(p, p + 2)));
            p += 2;
            t.setCategoryCode(Integer.parseInt(data.substring(p, p + 4).trim()));
            p += 4;
            t.setSource(trimField(data.substring(p, p + 10)));
            p += 10;
            t.setDescription(trimField(data.substring(p, p + 100)));
            p += 100;
            t.setAmount(parseImpliedDecimal12(data.substring(p, p + 12)));
            p += 12;
            t.setMerchantId(Long.parseLong(data.substring(p, p + 9).trim()));
            p += 9;
            t.setMerchantName(trimField(data.substring(p, p + 50)));
            p += 50;
            t.setMerchantCity(trimField(data.substring(p, p + 50)));
            p += 50;
            t.setMerchantZip(trimField(data.substring(p, p + 10)));
            p += 10;
            t.setCardNumber(trimField(data.substring(p, p + 16)));
            p += 16;
            t.setOriginTimestamp(trimField(data.substring(p, p + 26)));
            p += 26;
            t.setProcessedTimestamp(trimField(data.substring(p, p + 26)));
            return t;
        } catch (RuntimeException ex) {
            return new InvalidImportRecord(preview(fullLine), "T", "Transaction parse error: " + ex.getMessage());
        }
    }

    private static Object parseCard(String data, String fullLine) {
        try {
            if (data.length() < 91) {
                return new InvalidImportRecord(preview(fullLine), "D", "Card payload truncated");
            }
            Card c = Card.forImport();
            c.setCardNumber(trimField(data.substring(0, 16)));
            c.setAccountId(Long.parseLong(data.substring(16, 27).trim()));
            c.setCvvCode(Integer.parseInt(data.substring(27, 30).trim()));
            c.setEmbossedName(trimField(data.substring(30, 80)));
            c.setExpirationDate(trimField(data.substring(80, 90)));
            c.setActiveStatus(trimField(data.substring(90, 91)));
            return c;
        } catch (RuntimeException ex) {
            return new InvalidImportRecord(preview(fullLine), "D", "Card parse error: " + ex.getMessage());
        }
    }

    private static String trimField(String raw) {
        return StringPaddingUtil.trimTrailing(raw);
    }

    /**
     * Inverse of {@link AccountLineAggregator#formatImpliedDecimalMoney(java.math.BigDecimal)}:
     * 12 character COBOL-style implied decimal.
     */
    private static BigDecimal parseImpliedDecimal12(String field) {
        String s = field.trim();
        if (s.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        }
        boolean negative = s.startsWith("-");
        String digits = negative ? s.substring(1).trim() : s;
        if (digits.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        }
        BigInteger unscaled = new BigInteger(digits);
        BigDecimal v = new BigDecimal(unscaled, 2);
        return negative ? v.negate() : v;
    }
}

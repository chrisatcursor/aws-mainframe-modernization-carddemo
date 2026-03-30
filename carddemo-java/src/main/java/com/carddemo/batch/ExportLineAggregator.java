package com.carddemo.batch;

import com.carddemo.account.Account;
import com.carddemo.account.Customer;
import com.carddemo.card.Card;
import com.carddemo.card.CardXref;
import com.carddemo.transaction.Transaction;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.stereotype.Component;

/**
 * Formats {@link ExportLineEnvelope} into a fixed 500-byte COBOL-style line: type (C/A/X/T/D per
 * CBEXPORT), CVEXPORT header fields, and a 455-character payload derived from CVEXPORT redefines.
 */
@Component
public class ExportLineAggregator implements LineAggregator<ExportLineEnvelope> {

    private static final int PAYLOAD_LEN = 455;

    @Override
    public String aggregate(ExportLineEnvelope item) {
        char typeChar;
        String payload = switch (item.entity()) {
            case Customer c -> {
                typeChar = 'C';
                yield customerPayload(c);
            }
            case Account a -> {
                typeChar = 'A';
                yield accountPayload(a);
            }
            case CardXref x -> {
                typeChar = 'X';
                yield xrefPayload(x);
            }
            case Transaction t -> {
                typeChar = 'T';
                yield transactionPayload(t);
            }
            case Card d -> {
                typeChar = 'D';
                yield cardPayload(d);
            }
            default -> throw new IllegalStateException("Unsupported export entity: " + item.entity().getClass());
        };
        String prefix = ""
                + typeChar
                + item.exportTimestamp()
                + String.format(Locale.US, "%09d", item.sequence())
                + DataExportStepContext.BRANCH_ID
                + text(DataExportStepContext.REGION_CODE, 5);
        return fixed500(prefix + padRight(payload, PAYLOAD_LEN));
    }

    private static String customerPayload(Customer c) {
        StringBuilder sb = new StringBuilder(PAYLOAD_LEN);
        sb.append(fmtUnsignedLong(c.getCustId(), 9));
        sb.append(text(c.getFirstName(), 25));
        sb.append(text(c.getMiddleName(), 25));
        sb.append(text(c.getLastName(), 25));
        sb.append(text(c.getAddressLine1(), 50));
        sb.append(text(c.getAddressLine2(), 50));
        sb.append(text(c.getAddressLine3(), 50));
        sb.append(text(c.getAddressStateCode(), 2));
        sb.append(text(c.getAddressCountryCode(), 3));
        sb.append(text(c.getAddressZip(), 10));
        sb.append(text(c.getPhoneNumber1(), 15));
        sb.append(text(c.getPhoneNumber2(), 15));
        sb.append(fmtUnsignedLong(c.getSsn(), 9));
        sb.append(text(c.getGovtIssuedId(), 20));
        sb.append(text(c.getDateOfBirth(), 10));
        sb.append(text(c.getEftAccountId(), 10));
        sb.append(text(c.getPrimaryCardHolderIndicator(), 1));
        sb.append(fmtUnsignedInt(c.getFicoCreditScore(), 3));
        sb.append(" ".repeat(Math.max(0, PAYLOAD_LEN - sb.length())));
        return sb.substring(0, PAYLOAD_LEN);
    }

    private static String accountPayload(Account a) {
        StringBuilder sb = new StringBuilder(PAYLOAD_LEN);
        sb.append(fmtUnsignedLong(a.getAcctId(), 11));
        sb.append(text(a.getActiveStatus(), 1));
        sb.append(fmtMoney(a.getCurrentBalance(), 13));
        sb.append(fmtMoney(a.getCreditLimit(), 13));
        sb.append(fmtMoney(a.getCashCreditLimit(), 13));
        sb.append(text(a.getOpenDate(), 10));
        sb.append(text(a.getExpirationDate(), 10));
        sb.append(text(a.getReissueDate(), 10));
        sb.append(fmtMoney(a.getCurrentCycleCredit(), 13));
        sb.append(fmtMoney(a.getCurrentCycleDebit(), 13));
        sb.append(text(a.getAddressZip(), 10));
        sb.append(text(a.getGroupId(), 10));
        sb.append(" ".repeat(Math.max(0, PAYLOAD_LEN - sb.length())));
        return sb.substring(0, PAYLOAD_LEN);
    }

    private static String xrefPayload(CardXref x) {
        StringBuilder sb = new StringBuilder(PAYLOAD_LEN);
        sb.append(text(x.getCardNumber(), 16));
        sb.append(fmtUnsignedLong(x.getCustomerId(), 9));
        sb.append(fmtUnsignedLong(x.getAccountId(), 11));
        sb.append(" ".repeat(Math.max(0, PAYLOAD_LEN - sb.length())));
        return sb.substring(0, PAYLOAD_LEN);
    }

    private static String transactionPayload(Transaction t) {
        StringBuilder sb = new StringBuilder(PAYLOAD_LEN);
        sb.append(text(t.getTransactionId(), 16));
        sb.append(text(t.getTypeCode(), 2));
        sb.append(fmtUnsignedInt(t.getCategoryCode(), 4));
        sb.append(text(t.getSource(), 10));
        sb.append(text(t.getDescription(), 100));
        sb.append(fmtMoney(t.getAmount(), 12));
        sb.append(fmtUnsignedLong(t.getMerchantId(), 9));
        sb.append(text(t.getMerchantName(), 50));
        sb.append(text(t.getMerchantCity(), 50));
        sb.append(text(t.getMerchantZip(), 10));
        sb.append(text(t.getCardNumber(), 16));
        sb.append(text(t.getOriginTimestamp(), 26));
        sb.append(text(t.getProcessedTimestamp(), 26));
        sb.append(" ".repeat(Math.max(0, PAYLOAD_LEN - sb.length())));
        return sb.substring(0, PAYLOAD_LEN);
    }

    private static String cardPayload(Card c) {
        StringBuilder sb = new StringBuilder(PAYLOAD_LEN);
        sb.append(text(c.getCardNumber(), 16));
        sb.append(fmtUnsignedLong(c.getAccountId(), 11));
        sb.append(fmtUnsignedInt(c.getCvvCode(), 3));
        sb.append(text(c.getEmbossedName(), 50));
        sb.append(text(c.getExpirationDate(), 10));
        sb.append(text(c.getActiveStatus(), 1));
        sb.append(" ".repeat(Math.max(0, PAYLOAD_LEN - sb.length())));
        return sb.substring(0, PAYLOAD_LEN);
    }

    private static String fixed500(String line) {
        if (line.length() >= DataExportStepContext.RECORD_LENGTH) {
            return line.substring(0, DataExportStepContext.RECORD_LENGTH);
        }
        return line + " ".repeat(DataExportStepContext.RECORD_LENGTH - line.length());
    }

    private static String padRight(String s, int len) {
        if (s.length() >= len) {
            return s.substring(0, len);
        }
        return s + " ".repeat(len - s.length());
    }

    private static String text(String s, int len) {
        if (s == null) {
            return " ".repeat(len);
        }
        if (s.length() >= len) {
            return s.substring(0, len);
        }
        return s + " ".repeat(len - s.length());
    }

    private static String fmtUnsignedLong(Long n, int width) {
        if (n == null) {
            return "0".repeat(width);
        }
        String digits = Long.toUnsignedString(Math.max(0, n));
        if (digits.length() > width) {
            return digits.substring(digits.length() - width);
        }
        return "0".repeat(width - digits.length()) + digits;
    }

    private static String fmtUnsignedInt(Integer n, int width) {
        if (n == null) {
            return "0".repeat(width);
        }
        return fmtUnsignedLong((long) Math.max(0, n), width);
    }

    private static String fmtMoney(BigDecimal amount, int width) {
        BigDecimal a = amount == null ? BigDecimal.ZERO : amount;
        String s = String.format(Locale.US, "%01.2f", a);
        if (s.length() > width) {
            return s.substring(0, width);
        }
        return " ".repeat(width - s.length()) + s;
    }
}

package com.carddemo.batch;

import com.carddemo.account.Account;
import com.carddemo.common.DateTimeHelper;
import com.carddemo.common.StringPaddingUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.batch.item.file.transform.LineAggregator;

/**
 * Formats {@link Account} rows for CBACT01C outputs: OUTFILE (fixed-width), ARRYFILE (tabular),
 * VBRCFILE (two logical variable records per account, expressed as two lines).
 */
public final class AccountLineAggregator implements LineAggregator<Account> {

    /** Total length of OUT-ACCT-REC in CBACT01C (FD OUT-FILE): 11+1+5×12+3×10+10. */
    public static final int FIXED_WIDTH_RECORD_LENGTH = 112;

    public static final int FIELD_ACCT_ID_LEN = 11;
    public static final int FIELD_ACTIVE_STATUS_LEN = 1;
    public static final int FIELD_MONEY_LEN = 12;
    public static final int FIELD_DATE_LEN = 10;
    public static final int FIELD_GROUP_LEN = 10;

    public enum FormatMode {
        FIXED_WIDTH,
        ARRAY,
        VARIABLE
    }

    private final FormatMode mode;

    public AccountLineAggregator(FormatMode mode) {
        this.mode = Objects.requireNonNull(mode);
    }

    @Override
    public String aggregate(Account account) {
        return switch (mode) {
            case FIXED_WIDTH -> toFixedWidth(account);
            case ARRAY -> toArrayLine(account);
            case VARIABLE -> toVariableBlock(account);
        };
    }

    private static String toFixedWidth(Account a) {
        BigDecimal outDebit = a.getCurrentCycleDebit();
        if (outDebit == null || outDebit.compareTo(BigDecimal.ZERO) == 0) {
            outDebit = new BigDecimal("2525.00");
        }
        String reissueOut = formatReissueDateForOutfile(a.getReissueDate());
        String line =
                StringPaddingUtil.zeroPad(a.getAcctId(), FIELD_ACCT_ID_LEN)
                        + StringPaddingUtil.rightPad(nvl(a.getActiveStatus()), FIELD_ACTIVE_STATUS_LEN)
                        + formatImpliedDecimalMoney(a.getCurrentBalance())
                        + formatImpliedDecimalMoney(a.getCreditLimit())
                        + formatImpliedDecimalMoney(a.getCashCreditLimit())
                        + StringPaddingUtil.rightPad(nvl(a.getOpenDate()), FIELD_DATE_LEN)
                        + StringPaddingUtil.rightPad(nvl(a.getExpirationDate()), FIELD_DATE_LEN)
                        + StringPaddingUtil.rightPad(reissueOut, FIELD_DATE_LEN)
                        + formatImpliedDecimalMoney(a.getCurrentCycleCredit())
                        + formatImpliedDecimalMoney(outDebit)
                        + StringPaddingUtil.rightPad(nvl(a.getGroupId()), FIELD_GROUP_LEN);
        if (line.length() != FIXED_WIDTH_RECORD_LENGTH) {
            throw new IllegalStateException(
                    "Fixed-width line length " + line.length() + " != " + FIXED_WIDTH_RECORD_LENGTH);
        }
        return line;
    }

    /**
     * Mirrors 1400-POPUL-ARRAY-REC: id plus five (balance, cyc-debit) pairs; only first three
     * pairs are populated as in COBOL; remainder are zero. Trailing ARR-FILLER is four spaces.
     */
    private static String toArrayLine(Account a) {
        String id = StringPaddingUtil.zeroPad(a.getAcctId(), FIELD_ACCT_ID_LEN);
        BigDecimal bal = a.getCurrentBalance() != null ? a.getCurrentBalance() : BigDecimal.ZERO;
        List<String> parts = new ArrayList<>();
        parts.add(id);
        parts.add(bal.toPlainString());
        parts.add("1005.00");
        parts.add(bal.toPlainString());
        parts.add("1525.00");
        parts.add("-1025.00");
        parts.add("-2500.00");
        parts.add(BigDecimal.ZERO.toPlainString());
        parts.add(BigDecimal.ZERO.toPlainString());
        parts.add(BigDecimal.ZERO.toPlainString());
        parts.add(BigDecimal.ZERO.toPlainString());
        parts.add("    ");
        return String.join("|", parts);
    }

    /**
     * Two variable records per account (1550 + 1575): VB1 (id + status) and VB2 (id, balances,
     * reissue year). Emitted as two lines joined by a newline; each line is CSV of non-blank
     * fields only.
     */
    private static String toVariableBlock(Account a) {
        String vb1 = csvNonEmpty(
                StringPaddingUtil.zeroPad(a.getAcctId(), FIELD_ACCT_ID_LEN), nvl(a.getActiveStatus()));
        String year = extractReissueYear(a.getReissueDate());
        String vb2 = csvNonEmpty(
                StringPaddingUtil.zeroPad(a.getAcctId(), FIELD_ACCT_ID_LEN),
                plainMoney(a.getCurrentBalance()),
                plainMoney(a.getCreditLimit()),
                year);
        return vb1 + "\n" + vb2;
    }

    private static String csvNonEmpty(String... values) {
        return java.util.Arrays.stream(values)
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.joining(","));
    }

    private static String plainMoney(BigDecimal v) {
        if (v == null) {
            return null;
        }
        return v.stripTrailingZeros().toPlainString();
    }

    private static String formatReissueDateForOutfile(String reissueDate) {
        if (reissueDate == null || reissueDate.isBlank()) {
            return "";
        }
        String trimmed = reissueDate.trim();
        LocalDate parsed = DateTimeHelper.parseCobolDate(trimmed);
        if (parsed != null) {
            return DateTimeHelper.toDisplayDate(parsed);
        }
        return trimmed;
    }

    private static String extractReissueYear(String reissueDate) {
        if (reissueDate == null || reissueDate.isBlank()) {
            return null;
        }
        String trimmed = reissueDate.trim();
        LocalDate parsed = DateTimeHelper.parseCobolDate(trimmed);
        if (parsed != null) {
            return String.valueOf(parsed.getYear());
        }
        if (trimmed.length() >= 4) {
            return trimmed.substring(0, 4);
        }
        return null;
    }

    /**
     * COBOL PIC S9(10)V99 display: 12 character positions with implied decimal (no {@code .} in
     * output); leading {@code -} when negative (11 digit positions after sign).
     */
    static String formatImpliedDecimalMoney(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        boolean negative = scaled.signum() < 0;
        String digits = scaled.abs().unscaledValue().toString();
        if (digits.length() > 12) {
            digits = digits.substring(digits.length() - 12);
        } else {
            digits = "0".repeat(12 - digits.length()) + digits;
        }
        if (negative) {
            return "-" + digits.substring(0, 11);
        }
        return digits;
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}

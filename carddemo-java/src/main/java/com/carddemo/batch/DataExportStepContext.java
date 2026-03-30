package com.carddemo.batch;

import com.carddemo.account.Account;
import com.carddemo.account.Customer;
import com.carddemo.card.Card;
import com.carddemo.card.CardXref;
import com.carddemo.transaction.Transaction;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.Chunk;

/**
 * Per-step export metadata (COBOL WS-FORMATTED-TIMESTAMP / sequence), header and trailer lines, and
 * per-entity write counts for the trailer.
 */
public class DataExportStepContext implements StepExecutionListener, ItemWriteListener<ExportLineEnvelope> {

    public static final int RECORD_LENGTH = 500;
    public static final String BRANCH_ID = "0001";
    public static final String REGION_CODE = "NORTH";

    private final AtomicInteger sequence = new AtomicInteger(0);
    private String exportTimestamp26;
    private String exportDateYyyyMmDd;
    private long customerCount;
    private long accountCount;
    private long xrefCount;
    private long transactionCount;
    private long cardCount;

    public String getExportTimestamp26() {
        return exportTimestamp26;
    }

    public String getExportDateYyyyMmDd() {
        return exportDateYyyyMmDd;
    }

    public int nextSequence() {
        return sequence.incrementAndGet();
    }

    public long getCustomerCount() {
        return customerCount;
    }

    public long getAccountCount() {
        return accountCount;
    }

    public long getXrefCount() {
        return xrefCount;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public long getCardCount() {
        return cardCount;
    }

    public long getTotalDetailCount() {
        return customerCount + accountCount + xrefCount + transactionCount + cardCount;
    }

    public String buildHeaderLine() {
        String body = "H" + exportDateYyyyMmDd;
        return padRight(body, RECORD_LENGTH);
    }

    public String buildFooterLine() {
        long total = getTotalDetailCount();
        String body = "Z"
                + fmtCount(customerCount)
                + fmtCount(accountCount)
                + fmtCount(xrefCount)
                + fmtCount(transactionCount)
                + fmtCount(cardCount)
                + fmtCount(total);
        return padRight(body, RECORD_LENGTH);
    }

    private static String fmtCount(long n) {
        return String.format(Locale.US, "%09d", Math.min(n, 999_999_999L));
    }

    private static String padRight(String s, int len) {
        if (s.length() >= len) {
            return s.substring(0, len);
        }
        return s + " ".repeat(len - s.length());
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        sequence.set(0);
        customerCount = 0;
        accountCount = 0;
        xrefCount = 0;
        transactionCount = 0;
        cardCount = 0;
        ZonedDateTime now = ZonedDateTime.now();
        exportDateYyyyMmDd = now.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String timePart = String.format(
                Locale.US, "%02d:%02d:%02d.00", now.getHour(), now.getMinute(), now.getSecond());
        String raw = exportDateYyyyMmDd + " " + timePart;
        exportTimestamp26 = padRight(raw, 26);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return stepExecution.getExitStatus();
    }

    @Override
    public void beforeWrite(Chunk<? extends ExportLineEnvelope> chunk) {
        // no-op
    }

    @Override
    public void afterWrite(Chunk<? extends ExportLineEnvelope> chunk) {
        for (ExportLineEnvelope env : chunk.getItems()) {
            switch (env.entity()) {
                case Customer c -> customerCount++;
                case Account a -> accountCount++;
                case CardXref x -> xrefCount++;
                case Transaction t -> transactionCount++;
                case Card d -> cardCount++;
                default -> throw new IllegalStateException("Unexpected export entity: " + env.entity().getClass());
            }
        }
    }

    @Override
    public void onWriteError(Exception exception, Chunk<? extends ExportLineEnvelope> chunk) {
        // no-op
    }
}

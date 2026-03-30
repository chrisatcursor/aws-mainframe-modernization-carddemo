package com.carddemo.batch;

import static com.carddemo.batch.TransactionReportProcessor.CODE_DESC_SEPARATOR;

import com.carddemo.common.StringPaddingUtil;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;

/**
 * Paginated fixed-width report writer for TRANREPT (CVTRA07Y layout, 133-character lines).
 */
public class TransactionReportWriter implements ItemStreamWriter<TransactionReportLine> {

    /** Default detail lines per page (overridable via job parameter {@code linesPerPage}). */
    public static final int DEFAULT_LINES_PER_PAGE = 60;

    private final int linesPerPage;

    private static final int RECORD_WIDTH = 133;

    private static final String HEADER_1 = StringPaddingUtil.rightPad("Transaction ID", 17)
            + StringPaddingUtil.rightPad("Account ID", 12)
            + StringPaddingUtil.rightPad("Transaction Type", 19)
            + StringPaddingUtil.rightPad("Tran Category", 35)
            + StringPaddingUtil.rightPad("Tran Source", 14)
            + " "
            + StringPaddingUtil.rightPad("Amount", 16);

    private static final String HEADER_2 = "-".repeat(RECORD_WIDTH);

    private final String outputFile;
    private final String startDate;
    private final String endDate;

    private final DecimalFormat detailAmountFormat;
    private final DecimalFormat totalAmountFormat;

    private PrintWriter out;
    private int pageNumber = 1;
    private int detailLinesOnPage;
    private BigDecimal pageTotal = BigDecimal.ZERO;
    private BigDecimal runningGrandTotal = BigDecimal.ZERO;
    private boolean wroteAnyDetail;

    public TransactionReportWriter(String outputFile, String startDate, String endDate, int linesPerPage) {
        this.outputFile = outputFile;
        this.startDate = startDate != null ? startDate : "";
        this.endDate = endDate != null ? endDate : "";
        this.linesPerPage = linesPerPage > 0 ? linesPerPage : DEFAULT_LINES_PER_PAGE;

        DecimalFormatSymbols syms = DecimalFormatSymbols.getInstance(Locale.US);
        this.detailAmountFormat = new DecimalFormat("#,##0.00", syms);
        this.detailAmountFormat.setPositivePrefix("");
        this.detailAmountFormat.setNegativePrefix("-");

        this.totalAmountFormat = new DecimalFormat("#,##0.00", syms);
        this.totalAmountFormat.setPositivePrefix("+");
        this.totalAmountFormat.setNegativePrefix("-");
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            Path path = Path.of(outputFile);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            this.out = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8));
            writePageHeaderBlock();
            this.detailLinesOnPage = 0;
            this.pageTotal = BigDecimal.ZERO;
            this.runningGrandTotal = BigDecimal.ZERO;
            this.wroteAnyDetail = false;
        } catch (IOException e) {
            throw new ItemStreamException("Could not open report file: " + outputFile, e);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        // restart not supported
    }

    @Override
    public void write(Chunk<? extends TransactionReportLine> chunk) throws Exception {
        for (TransactionReportLine line : chunk.getItems()) {
            if (detailLinesOnPage >= linesPerPage) {
                endPage(true);
            }
            out.println(formatDetailLine(line));
            pageTotal = pageTotal.add(line.amount());
            detailLinesOnPage++;
            wroteAnyDetail = true;
        }
        out.flush();
    }

    @Override
    public void close() throws ItemStreamException {
        if (out == null) {
            return;
        }
        try {
            if (wroteAnyDetail) {
                endPage(false);
                writeGrandTotalLine();
            }
        } finally {
            out.close();
            out = null;
        }
    }

    /**
     * @param advancePage if true, start a new page after the page total (mid-report pagination);
     *     if false, only emit the page total line (used for the final page before grand total).
     */
    private void endPage(boolean advancePage) {
        writePageTotalLine(pageTotal);
        runningGrandTotal = runningGrandTotal.add(pageTotal);
        pageTotal = BigDecimal.ZERO;
        detailLinesOnPage = 0;
        if (advancePage) {
            pageNumber++;
            writePageHeaderBlock();
        }
    }

    private void writePageHeaderBlock() {
        String title = buildTitleLine();
        out.println(truncateOrPad(title, RECORD_WIDTH));
        out.println(truncateOrPad("", RECORD_WIDTH));
        out.println(truncateOrPad(HEADER_1, RECORD_WIDTH));
        out.println(truncateOrPad(HEADER_2, RECORD_WIDTH));
    }

    private String buildTitleLine() {
        String base = StringPaddingUtil.rightPad("DALYREPT", 38)
                + StringPaddingUtil.rightPad("Daily Transaction Report", 41)
                + StringPaddingUtil.rightPad("Date Range: ", 12)
                + StringPaddingUtil.rightPad(startDate, 10)
                + " to "
                + StringPaddingUtil.rightPad(endDate, 10)
                + " Page: "
                + pageNumber;
        return base;
    }

    private void writePageTotalLine(BigDecimal amount) {
        String dots = ".".repeat(86);
        String label = StringPaddingUtil.rightPad("Page Total", 11);
        String amt = StringPaddingUtil.leftPad(totalAmountFormat.format(amount), 16);
        out.println(truncateOrPad(label + dots + amt, RECORD_WIDTH));
    }

    private void writeGrandTotalLine() {
        String dots = ".".repeat(86);
        String label = StringPaddingUtil.rightPad("Grand Total", 11);
        String amt = StringPaddingUtil.leftPad(totalAmountFormat.format(runningGrandTotal), 16);
        out.println(truncateOrPad(label + dots + amt, RECORD_WIDTH));
    }

    private String formatDetailLine(TransactionReportLine line) {
        String[] typeParts = splitCodeDesc(line.transactionType());
        String[] catParts = splitCodeDesc(line.transactionCategory());

        String typeCd = StringPaddingUtil.rightPad(typeParts[0], 2);
        String typeDesc = StringPaddingUtil.rightPad(typeParts[1], 15);
        String catCd = StringPaddingUtil.leftPad(catParts[0], 4);
        String catDesc = StringPaddingUtil.rightPad(catParts[1], 29);

        String amtStr = StringPaddingUtil.leftPad(detailAmountFormat.format(line.amount()), 16);

        String body = StringPaddingUtil.rightPad(line.transactionId(), 16)
                + " "
                + StringPaddingUtil.leftPad(line.accountId(), 11)
                + " "
                + typeCd
                + "-"
                + typeDesc
                + " "
                + catCd
                + "-"
                + catDesc
                + " "
                + StringPaddingUtil.rightPad(line.description(), 10)
                + "    "
                + amtStr
                + "  ";
        return truncateOrPad(body, RECORD_WIDTH);
    }

    private static String[] splitCodeDesc(String field) {
        if (field == null) {
            return new String[] {"", ""};
        }
        int sep = field.indexOf(CODE_DESC_SEPARATOR);
        if (sep < 0) {
            return new String[] {"", field};
        }
        return new String[] {field.substring(0, sep), field.substring(sep + 1)};
    }

    private static String truncateOrPad(String line, int width) {
        if (line.length() >= width) {
            return line.substring(0, width);
        }
        return line + " ".repeat(width - line.length());
    }
}

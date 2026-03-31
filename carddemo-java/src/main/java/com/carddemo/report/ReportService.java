package com.carddemo.report;

import com.carddemo.common.DateValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private static final String ISO_DATE_PATTERN = "yyyy-MM-dd";

    public ReportResult submitReport(String startDate, String endDate) {
        if (startDate == null || startDate.isBlank()) {
            return ReportResult.error("Start date is required");
        }
        if (endDate == null || endDate.isBlank()) {
            return ReportResult.error("End date is required");
        }
        if (!isValidIsoDate(startDate)) {
            return ReportResult.error("Invalid start date format. Use YYYY-MM-DD.");
        }
        if (!isValidIsoDate(endDate)) {
            return ReportResult.error("Invalid end date format. Use YYYY-MM-DD.");
        }
        if (startDate.compareTo(endDate) > 0) {
            return ReportResult.error("Start date must be before end date");
        }

        log.info("Transaction report submitted for date range {} to {}", startDate, endDate);
        return ReportResult.success("Transaction report request submitted for " + startDate + " to " + endDate);
    }

    private static boolean isValidIsoDate(String date) {
        return DateValidator.validate(date, ISO_DATE_PATTERN).severity() == 0;
    }

    public record ReportResult(boolean success, String message) {
        public static ReportResult success(String message) {
            return new ReportResult(true, message);
        }

        public static ReportResult error(String message) {
            return new ReportResult(false, message);
        }
    }
}

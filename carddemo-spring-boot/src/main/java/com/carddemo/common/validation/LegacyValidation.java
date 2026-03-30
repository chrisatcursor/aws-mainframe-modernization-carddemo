package com.carddemo.common.validation;

import com.carddemo.common.error.ValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class LegacyValidation {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\(\\d{3}\\)\\d{3}-\\d{4}$");
    private static final Pattern STATE_PATTERN = Pattern.compile("^[A-Z]{2}$");
    private static final Pattern SSN_PATTERN = Pattern.compile("^\\d{9}$");
    private static final Pattern CARD_NAME_PATTERN = Pattern.compile("^[A-Za-z ]+$");
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^\\d{16}$");

    private LegacyValidation() {
    }

    public static boolean isValidAccountId(Long accountId) {
        return accountId != null && accountId > 0 && accountId <= 99_999_999_999L;
    }

    public static boolean isValidCardNumber(String cardNumber) {
        return cardNumber != null && CARD_NUMBER_PATTERN.matcher(cardNumber.trim()).matches();
    }

    public static boolean isValidYnFlag(String value) {
        return value != null && ("Y".equalsIgnoreCase(value.trim()) || "N".equalsIgnoreCase(value.trim()));
    }

    public static boolean isAlphabeticSpace(String value) {
        return value != null && !value.isBlank() && CARD_NAME_PATTERN.matcher(value.trim()).matches();
    }

    public static void requireStatusYn(String value, String field) {
        if (value == null) {
            return;
        }
        if (!isValidYnFlag(value)) {
            throw new ValidationException(field + " must be Y or N");
        }
    }

    public static void requireYn(String value, String field) {
        requireStatusYn(value, field);
    }

    public static void requireStateCode(String value, String field) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!STATE_PATTERN.matcher(value.trim()).matches()) {
            throw new ValidationException(field + " must be two uppercase letters");
        }
    }

    public static void requirePhone(String value, String field) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!PHONE_PATTERN.matcher(value.trim()).matches()) {
            throw new ValidationException(field + " must match (###)###-####");
        }
    }

    public static void requireSsn(String value, String field) {
        if (value == null || value.isBlank()) {
            return;
        }
        String trimmed = value.trim();
        if (!SSN_PATTERN.matcher(trimmed).matches()) {
            throw new ValidationException(field + " must be 9 digits");
        }
        int part1 = Integer.parseInt(trimmed.substring(0, 3));
        if (part1 == 0 || part1 == 666 || part1 >= 900) {
            throw new ValidationException(field + " has invalid first segment");
        }
    }

    public static void requireFico(Integer value, String field) {
        if (value == null) {
            return;
        }
        if (value < 0 || value > 999) {
            throw new ValidationException(field + " must be between 000 and 999");
        }
    }

    public static void requireCardName(String value, String field) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!CARD_NAME_PATTERN.matcher(value.trim()).matches()) {
            throw new ValidationException(field + " can only contain alphabets and spaces");
        }
    }

    public static void requireAlphabeticSpace(String value, String field) {
        requireCardName(value, field);
    }

    public static void requireCardExpiry(Integer month, Integer year) {
        if (month == null && year == null) {
            return;
        }
        if (month == null || year == null) {
            throw new ValidationException("expiryMonth and expiryYear must be provided together");
        }
        if (month < 1 || month > 12) {
            throw new ValidationException("Card expiry month must be between 1 and 12");
        }
        if (year < 1950 || year > 2099) {
            throw new ValidationException("Invalid card expiry year");
        }
    }

    public static void requireMonthRange(int month, String field) {
        if (month < 1 || month > 12) {
            throw new ValidationException(field + " must be between 1 and 12");
        }
    }

    public static void requireYearRange(int year, String field) {
        if (year < 1950 || year > 2099) {
            throw new ValidationException(field + " must be between 1950 and 2099");
        }
    }

    public static void requirePositiveMoney(BigDecimal value, String field) {
        if (value == null) {
            throw new ValidationException(field + " is required");
        }
        if (value.signum() < 0) {
            throw new ValidationException(field + " cannot be negative");
        }
    }

    public static void requireDate(LocalDate value, String field) {
        if (value == null) {
            throw new ValidationException(field + " is required");
        }
    }

    public static void requireDate(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
        try {
            LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new ValidationException(field + " must be in yyyy-MM-dd format");
        }
    }

    public static void requireNotNull(Object value, String message) {
        if (value == null) {
            throw new ValidationException(message);
        }
    }
}

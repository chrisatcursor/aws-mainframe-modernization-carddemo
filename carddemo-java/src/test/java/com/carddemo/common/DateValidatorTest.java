package com.carddemo.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DateValidatorTest {

    @Test
    void validIsoDate() {
        DateValidator.ValidationResult r = DateValidator.validate("2024-01-15", "yyyy-MM-dd");
        assertEquals(0, r.severity());
        assertEquals("Date is valid", r.message());
        assertNotNull(r.message());
    }

    @Test
    void validDisplayDateWithTwoDigitYear() {
        DateValidator.ValidationResult r = DateValidator.validate("01/15/24", "MM/dd/yy");
        assertEquals(0, r.severity());
        assertEquals("Date is valid", r.message());
        assertNotNull(r.message());
    }

    @Test
    void invalidMonth() {
        DateValidator.ValidationResult r = DateValidator.validate("2024-13-01", "yyyy-MM-dd");
        assertNotEquals(0, r.severity());
        assertEquals("Invalid month", r.message());
        assertNotNull(r.message());
    }

    @Test
    void invalidDayForMonth() {
        DateValidator.ValidationResult r = DateValidator.validate("2024-02-30", "yyyy-MM-dd");
        assertNotEquals(0, r.severity());
        assertEquals("Datevalue error", r.message());
        assertNotNull(r.message());
    }

    @Test
    void blankDate() {
        DateValidator.ValidationResult r = DateValidator.validate("   ", "yyyy-MM-dd");
        assertNotEquals(0, r.severity());
        assertEquals("Insufficient", r.message());
        assertNotNull(r.message());
    }

    @Test
    void emptyDate() {
        DateValidator.ValidationResult r = DateValidator.validate("", "yyyy-MM-dd");
        assertNotEquals(0, r.severity());
        assertEquals("Insufficient", r.message());
    }

    @Test
    void nullDate() {
        DateValidator.ValidationResult r = DateValidator.validate(null, "yyyy-MM-dd");
        assertNotEquals(0, r.severity());
        assertEquals("Insufficient", r.message());
        assertNotNull(r.message());
    }

    @Test
    void nonNumericDate() {
        DateValidator.ValidationResult r = DateValidator.validate("abcd-ef-gh", "yyyy-MM-dd");
        assertNotEquals(0, r.severity());
        assertEquals("Nonnumeric data", r.message());
        assertNotNull(r.message());
    }

    @Test
    void emptyFormatString() {
        DateValidator.ValidationResult r = DateValidator.validate("2024-01-15", "");
        assertNotEquals(0, r.severity());
        assertEquals("Bad Pic String", r.message());
        assertNotNull(r.message());
    }

    @Test
    void resultMessageAlwaysNonNull() {
        assertNotNull(DateValidator.validate("2024-01-15", "yyyy-MM-dd").message());
        assertNotNull(DateValidator.validate("bad", "yyyy-MM-dd").message());
        assertNotNull(DateValidator.validate("2024-01-15", "not-a-real-pattern-[[[").message());
    }
}

package com.carddemo.common;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /** COTRN02C / CORPT00C {@code WS-DATE-FORMAT VALUE 'YYYY-MM-DD'}. */
    @Test
    void cobolPicture_uppercaseYyyyMmDd_isValid() {
        DateValidator.ValidationResult r = DateValidator.validate("2024-01-15", "YYYY-MM-DD");
        assertEquals(0, r.severity());
        assertEquals("Date is valid", r.message());
    }

    /** CSUTLDPY {@code MOVE 'YYYYMMDD' TO WS-DATE-FORMAT}. */
    @Test
    void cobolPicture_compactYyyyMmDd_isValid() {
        DateValidator.ValidationResult r = DateValidator.validate("20240115", "YYYYMMDD");
        assertEquals(0, r.severity());
        assertEquals("Date is valid", r.message());
    }

    @Test
    void cobolPicture_mmDdYyyy_isValid() {
        DateValidator.ValidationResult r = DateValidator.validate("01/15/2024", "MM/DD/YYYY");
        assertEquals(0, r.severity());
        assertEquals("Date is valid", r.message());
    }

    /** Vstring-length / CEEDAYS insufficient data when input shorter than picture. */
    @Test
    void shortDateString_vsPicture_returnsInsufficient() {
        DateValidator.ValidationResult r = DateValidator.validate("2024-01-1", "YYYY-MM-DD");
        assertEquals(1, r.severity());
        assertEquals("Insufficient", r.message());
    }

    @Test
    void shortCompactDate_returnsInsufficient() {
        DateValidator.ValidationResult r = DateValidator.validate("2024011", "YYYYMMDD");
        assertEquals(1, r.severity());
        assertEquals("Insufficient", r.message());
    }

    /** IBM Lilian day 1 = 15 Oct 1582 (Gregorian start per LE). */
    @Test
    void lilianDay_oneIsLilianEpoch() {
        assertEquals(1, DateValidator.toLilianDay(DateValidator.LILIAN_EPOCH));
        assertEquals(DateValidator.LILIAN_EPOCH, DateValidator.fromLilianDay(1));
    }

    @Test
    void lilianRoundTrip_knownDate() {
        LocalDate d = LocalDate.of(2024, 1, 15);
        assertEquals(d, DateValidator.fromLilianDay(DateValidator.toLilianDay(d)));
    }

    @Test
    void fromLilianDay_rejectsNonPositive() {
        assertThrows(IllegalArgumentException.class, () -> DateValidator.fromLilianDay(0));
    }

    @Test
    void toLilianDay_rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> DateValidator.toLilianDay(null));
    }

    /** CEEDAYS {@code FC-INVALID-ERA} — dates before Lilian epoch. */
    @Test
    void dateBeforeGregorianStart_returnsInvalidEra() {
        DateValidator.ValidationResult r = DateValidator.validate("1582-10-14", "YYYY-MM-DD");
        assertEquals(1, r.severity());
        assertEquals("Invalid Era", r.message());
    }

    /** CEEDAYS {@code FC-YEAR-IN-ERA-ZERO} — proleptic year zero not accepted. */
    @Test
    void yearZero_returnsYearInEraZero() {
        DateValidator.ValidationResult r = DateValidator.validate("0000-06-15", "yyyy-MM-dd");
        assertEquals(1, r.severity());
        assertEquals("YearInEra is 0", r.message());
    }

    /**
     * CSUTLDTC 88-level feedback tokens (IBM LE) — ensures Java messages stay aligned with COBOL
     * {@code FC-*} hex conditions.
     */
    @Test
    void ceedaysFeedbackHex_matchesCsutldtc() {
        assertEquals("0000000000000000", DateValidator.DateValidationCode.VALID.ceedaysFeedbackHex());
        assertEquals("000309CB59C3C5C5", DateValidator.DateValidationCode.INSUFFICIENT_DATA.ceedaysFeedbackHex());
        assertEquals("000309CC59C3C5C5", DateValidator.DateValidationCode.BAD_DATE_VALUE.ceedaysFeedbackHex());
        assertEquals("000309CD59C3C5C5", DateValidator.DateValidationCode.INVALID_ERA.ceedaysFeedbackHex());
        assertEquals("000309D159C3C5C5", DateValidator.DateValidationCode.UNSUPPORTED_RANGE.ceedaysFeedbackHex());
        assertEquals("000309D559C3C5C5", DateValidator.DateValidationCode.INVALID_MONTH.ceedaysFeedbackHex());
        assertEquals("000309D659C3C5C5", DateValidator.DateValidationCode.BAD_FORMAT_STRING.ceedaysFeedbackHex());
        assertEquals("000309D859C3C5C5", DateValidator.DateValidationCode.NON_NUMERIC_DATA.ceedaysFeedbackHex());
        assertEquals("000309D959C3C5C5", DateValidator.DateValidationCode.YEAR_IN_ERA_ZERO.ceedaysFeedbackHex());
    }

    @Test
    void mapCeedaysPicture_yyyyMmDdAndDdSemantics() {
        assertEquals("uuuu-MM-dd", DateValidator.mapCeedaysPictureToJavaPattern("YYYY-MM-DD"));
        assertEquals("uuuuMMdd", DateValidator.mapCeedaysPictureToJavaPattern("YYYYMMDD"));
    }

    @Test
    void minimumCharsForPattern_isoAndCompact() {
        assertEquals(10, DateValidator.minimumCharsForPattern("uuuu-MM-dd"));
        assertEquals(8, DateValidator.minimumCharsForPattern("uuuuMMdd"));
    }

    @Test
    void validOnLilianEpochBoundary() {
        DateValidator.ValidationResult r = DateValidator.validate("1582-10-15", "YYYY-MM-DD");
        assertEquals(0, r.severity());
        assertTrue(DateValidator.toLilianDay(DateValidator.LILIAN_EPOCH) > 0);
    }
}

package com.carddemo.common;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeHelperTest {

    @Test
    void toCobolDate_formatsCorrectly() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        assertThat(DateTimeHelper.toCobolDate(date)).isEqualTo("20240315");
    }

    @Test
    void toDisplayDate_formatsCorrectly() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        assertThat(DateTimeHelper.toDisplayDate(date)).isEqualTo("03/15/24");
    }

    @Test
    void toTimestamp_formatsCorrectly() {
        LocalDateTime dt = LocalDateTime.of(2024, 3, 15, 14, 30, 45, 123456000);
        assertThat(DateTimeHelper.toTimestamp(dt)).isEqualTo("2024-03-15 14:30:45.123456");
    }

    @Test
    void parseCobolDate_valid() {
        LocalDate result = DateTimeHelper.parseCobolDate("20240315");
        assertThat(result).isEqualTo(LocalDate.of(2024, 3, 15));
    }

    @Test
    void parseCobolDate_invalid_returnsNull() {
        assertThat(DateTimeHelper.parseCobolDate("99999999")).isNull();
        assertThat(DateTimeHelper.parseCobolDate("")).isNull();
        assertThat(DateTimeHelper.parseCobolDate(null)).isNull();
    }

    @Test
    void isValidCobolDate_valid() {
        assertThat(DateTimeHelper.isValidCobolDate("20240315")).isTrue();
    }

    @Test
    void isValidCobolDate_invalid() {
        assertThat(DateTimeHelper.isValidCobolDate("20241301")).isFalse();
        assertThat(DateTimeHelper.isValidCobolDate("abcdefgh")).isFalse();
    }
}

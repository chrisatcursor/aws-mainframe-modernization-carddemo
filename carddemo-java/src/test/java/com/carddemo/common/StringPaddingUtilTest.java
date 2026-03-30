package com.carddemo.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringPaddingUtilTest {

    @Test
    void rightPad_shorterThanLength_padsWithSpaces() {
        assertThat(StringPaddingUtil.rightPad("ABC", 8)).isEqualTo("ABC     ");
    }

    @Test
    void rightPad_longerThanLength_truncates() {
        assertThat(StringPaddingUtil.rightPad("ABCDEFGH", 5)).isEqualTo("ABCDE");
    }

    @Test
    void rightPad_exactLength_unchanged() {
        assertThat(StringPaddingUtil.rightPad("ABCD", 4)).isEqualTo("ABCD");
    }

    @Test
    void rightPad_null_returnsSpaces() {
        assertThat(StringPaddingUtil.rightPad(null, 5)).isEqualTo("     ");
    }

    @Test
    void leftPad_shorterThanLength_padsWithSpaces() {
        assertThat(StringPaddingUtil.leftPad("123", 8)).isEqualTo("     123");
    }

    @Test
    void leftPad_longerThanLength_truncatesFromLeft() {
        assertThat(StringPaddingUtil.leftPad("12345678", 5)).isEqualTo("45678");
    }

    @Test
    void leftPad_null_returnsSpaces() {
        assertThat(StringPaddingUtil.leftPad(null, 5)).isEqualTo("     ");
    }

    @Test
    void zeroPad_padsWithZeros() {
        assertThat(StringPaddingUtil.zeroPad(42, 8)).isEqualTo("00000042");
    }

    @Test
    void zeroPad_valueExceedsLength_truncatesFromLeft() {
        assertThat(StringPaddingUtil.zeroPad(123456789, 5)).isEqualTo("56789");
    }

    @Test
    void trimTrailing_removesTrailingSpaces() {
        assertThat(StringPaddingUtil.trimTrailing("ABC     ")).isEqualTo("ABC");
    }

    @Test
    void trimTrailing_noTrailingSpaces_unchanged() {
        assertThat(StringPaddingUtil.trimTrailing("ABC")).isEqualTo("ABC");
    }

    @Test
    void trimTrailing_allSpaces_returnsEmpty() {
        assertThat(StringPaddingUtil.trimTrailing("     ")).isEmpty();
    }

    @Test
    void trimTrailing_null_returnsNull() {
        assertThat(StringPaddingUtil.trimTrailing(null)).isNull();
    }
}

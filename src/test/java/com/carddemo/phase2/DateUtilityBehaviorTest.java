package com.carddemo.phase2;

import com.carddemo.common.DateUtilityService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Minimal behavioral stand-in for CSUTLDTC (mainframe CEEDAYS): ISO date validation and epoch-day conversion.
 */
class DateUtilityBehaviorTest {

    private final DateUtilityService dates = new DateUtilityService();

    @Test
    void validIsoDateParses() {
        assertThat(dates.parseIso("2026-04-01")).isEqualTo(LocalDate.of(2026, 4, 1));
    }

    @Test
    void invalidDateRejected() {
        assertThat(dates.parseIsoOrNull("2026-13-40")).isNull();
    }

    @Test
    void epochDaysConsistent() {
        LocalDate d = LocalDate.of(2026, 1, 1);
        assertThat(dates.toEpochDay(d)).isEqualTo(d.toEpochDay());
    }
}

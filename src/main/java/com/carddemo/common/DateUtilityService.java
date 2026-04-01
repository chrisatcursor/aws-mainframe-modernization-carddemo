package com.carddemo.common;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class DateUtilityService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    public LocalDate parseIso(String text) {
        return LocalDate.parse(text.trim(), ISO);
    }

    public LocalDate parseIsoOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return parseIso(text);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    public long toEpochDay(LocalDate d) {
        return d.toEpochDay();
    }
}

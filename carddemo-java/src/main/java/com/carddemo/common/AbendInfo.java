package com.carddemo.common;

/**
 * Structured error information mapped from CSMSG02Y (ABEND-DATA).
 * Used to propagate error context through the application in place of
 * CICS ABEND handling.
 *
 * @param code    4-character error code
 * @param culprit originating program/class name (up to 8 characters)
 * @param reason  human-readable reason (up to 50 characters)
 * @param message full error message (up to 72 characters)
 */
public record AbendInfo(
    String code,
    String culprit,
    String reason,
    String message
) {}

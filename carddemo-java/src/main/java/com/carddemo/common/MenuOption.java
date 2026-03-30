package com.carddemo.common;

/**
 * A single menu entry, mapped from the COMEN02Y / COADM02Y copybook structure.
 *
 * @param number      option number displayed to the user
 * @param name        human-readable label
 * @param endpoint    Spring MVC endpoint path (replaces COBOL program name)
 * @param requiredRole minimum role needed ({@code ROLE_USER} or {@code ROLE_ADMIN})
 */
public record MenuOption(
    int number,
    String name,
    String endpoint,
    String requiredRole
) {
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
}

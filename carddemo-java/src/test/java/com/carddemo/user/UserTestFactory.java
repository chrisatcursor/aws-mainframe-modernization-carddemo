package com.carddemo.user;

/** Same-package access to {@link User} for tests (protected no-arg constructor). */
public final class UserTestFactory {

    private UserTestFactory() {}

    public static User newUser() {
        return new User();
    }
}

package com.carddemo.account;

/** Test helpers for {@link Account}; lives in this package to use the entity's protected constructor. */
public final class AccountTestFactory {

    private AccountTestFactory() {}

    public static Account newAccount() {
        return new Account();
    }
}

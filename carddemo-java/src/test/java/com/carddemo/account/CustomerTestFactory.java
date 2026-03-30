package com.carddemo.account;

/** Same-package access to {@link Customer} for tests (protected no-arg constructor). */
public final class CustomerTestFactory {

    private CustomerTestFactory() {}

    public static Customer newCustomer() {
        return new Customer();
    }
}

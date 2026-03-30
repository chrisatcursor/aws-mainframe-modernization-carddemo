package com.carddemo.migration.fixedwidth;

import com.carddemo.account.model.Customer;

public final class CustomerFixedWidthCodec {

    private CustomerFixedWidthCodec() {
    }

    public static Customer decode(String line) {
        String normalized = FixedWidthSupport.requireLength(line, 500, "custdata");
        Customer customer = new Customer();
        customer.setCustomerId(FieldParsers.numericLong(normalized, 0, 9));
        customer.setFirstName(FieldParsers.text(normalized, 9, 34));
        customer.setMiddleName(FieldParsers.text(normalized, 34, 59));
        customer.setLastName(FieldParsers.text(normalized, 59, 84));
        customer.setAddressLine1(FieldParsers.text(normalized, 84, 134));
        customer.setAddressLine2(FieldParsers.text(normalized, 134, 184));
        customer.setAddressLine3(FieldParsers.text(normalized, 184, 234));
        customer.setStateCode(FieldParsers.text(normalized, 234, 236));
        customer.setCountryCode(FieldParsers.text(normalized, 236, 239));
        customer.setZip(FieldParsers.text(normalized, 239, 249));
        customer.setPhone1(FieldParsers.text(normalized, 249, 264));
        customer.setPhone2(FieldParsers.text(normalized, 264, 279));
        customer.setSsn(FieldParsers.text(normalized, 279, 288));
        customer.setGovtIssuedId(FieldParsers.text(normalized, 288, 308));
        customer.setDateOfBirth(FieldParsers.date(normalized, 308, 318));
        customer.setEftAccountId(FieldParsers.text(normalized, 318, 328));
        customer.setPrimaryCardHolderInd(FieldParsers.text(normalized, 328, 329));
        customer.setFicoScore(FieldParsers.numericInt(normalized, 329, 332));
        return customer;
    }
}

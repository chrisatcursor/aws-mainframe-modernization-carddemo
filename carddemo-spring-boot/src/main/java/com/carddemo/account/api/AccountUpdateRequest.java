package com.carddemo.account.api;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountUpdateRequest(
        @NotBlank @Size(min = 1, max = 1) String activeStatus,
        @NotNull BigDecimal creditLimit,
        @NotNull BigDecimal cashCreditLimit,
        @NotNull LocalDate expirationDate,
        @NotNull LocalDate reissueDate,
        @NotBlank @Size(min = 25, max = 25) String firstName,
        @NotBlank @Size(min = 25, max = 25) String middleName,
        @NotBlank @Size(min = 25, max = 25) String lastName,
        @NotBlank @Size(max = 50) String addressLine1,
        @NotBlank @Size(max = 50) String addressLine2,
        @NotBlank @Size(max = 50) String addressLine3,
        @NotBlank @Size(min = 2, max = 2) String stateCode,
        @NotBlank @Size(min = 3, max = 3) String countryCode,
        @NotBlank @Size(min = 10, max = 10) String zip,
        @NotBlank @Size(min = 15, max = 15) String phone1,
        @NotBlank @Size(min = 15, max = 15) String phone2,
        @NotBlank @Size(min = 9, max = 9) String ssn,
        @NotNull LocalDate dateOfBirth,
        @NotNull Integer ficoScore,
        Long version
) {
}

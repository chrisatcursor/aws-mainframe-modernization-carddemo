package com.carddemo.card.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CardUpdateRequest(
        @NotNull Long accountId,
        @NotBlank String cardNumber,
        @NotNull Long expectedVersion,
        @NotNull Integer oldCvvCode,
        @NotBlank String oldEmbossedName,
        @NotBlank String oldExpiryYear,
        @NotBlank String oldExpiryMonth,
        @NotBlank String oldExpiryDay,
        @NotBlank String oldActiveStatus,
        Integer newCvvCode,
        String newEmbossedName,
        String newExpiryYear,
        String newExpiryMonth,
        String newExpiryDay,
        String newActiveStatus,
        String newCardNumber) {
}

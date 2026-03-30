package com.carddemo.card.api;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CardUpdateRequest(
        @NotBlank @Size(max = 50) String embossedName,
        @NotBlank @Size(min = 1, max = 1) String activeStatus,
        @NotNull LocalDate expirationDate,
        @NotBlank @Size(max = 50) String originalEmbossedName,
        @NotBlank @Size(min = 1, max = 1) String originalActiveStatus,
        @NotNull LocalDate originalExpirationDate
) {
}

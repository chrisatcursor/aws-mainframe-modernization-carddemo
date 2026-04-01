package com.carddemo.account.dto;

public record AccountUpdateResponse(boolean success, String message, Long newAccountVersion, Long newCustomerVersion) {
}

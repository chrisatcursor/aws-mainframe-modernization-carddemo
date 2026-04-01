package com.carddemo.card.dto;

public record CardUpdateResponse(boolean success, String message, Long newVersion) {
}
